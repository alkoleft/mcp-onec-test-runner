/*
 * This file is part of METR.
 *
 * Copyright (C) 2025 Aleksey Koryakin <alkoleft@gmail.com> and contributors.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * METR is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * METR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with METR.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.alkoleft.mcp.infrastructure.storage

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import io.github.alkoleft.mcp.application.actions.test.yaxunit.ChangeType
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

/**
 * Build state manager implementing Enhanced Hybrid Hash Detection algorithm.
 * Combines fast timestamp pre-filtering with accurate hash verification for optimal performance.
 */
private val logger = KotlinLogging.logger { }

@Component
class FileBuildStateManager(
    private val hashStorage: MapDbHashStorage,
    private val properties: ApplicationProperties,
) {
    // Performance tuning parameters
    private val maxConcurrentHashCalculations = 4

    @OptIn(ExperimentalCoroutinesApi::class)
    fun checkChanges(): ChangesSet {
        val startTime = Instant.now()
        logger.debug { "Анализ изменений: ${properties.basePath}" }

        try {
            // Phase 1: Fast timestamp pre-scan
            val candidateFiles =
                properties.sourceSet
                    .flatMap { scanForPotentialChanges(properties.basePath.resolve(it.path), it.name) }
                    .toSet()

            logger.debug { "Фаза 1: Найдено ${candidateFiles.size} потенциальных изменений после сканирования временных меток" }

            if (candidateFiles.isEmpty()) {
                logger.debug { "Потенциальные изменения не обнаружены - пропуск проверки хешей" }
                return emptyMap()
            }

            // Phase 2: Hash verification for potential changes
            val actualChanges = verifyChangesWithHashes(candidateFiles)

            val duration = java.time.Duration.between(startTime, Instant.now())
            logger.info {
                "Обнаружение изменений завершено за ${duration.toMillis()}мс: ${actualChanges.size} фактических изменений из ${candidateFiles.size} кандидатов"
            }

            return actualChanges
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при обнаружении изменений" }
            // Fallback: treat all source files as changed
            return getAllSourceFiles(properties.basePath).associateWith { Pair(ChangeType.MODIFIED, "") }
        }
    }

    fun updateHashes(files: Map<Path, String>) {
        try {
            hashStorage.batchUpdate(files)
        } catch (e: Exception) {
            logger.error(e) { "Не удалось обновить хеши файлов" }
            throw e
        }
    }

    fun storeTimestamp(
        sourceSetName: String,
        timeStamp: Long,
    ) {
        hashStorage.storeTimestamp(sourceSetName, timeStamp)
    }

    /**
     * Phase 1: Fast timestamp pre-scan to identify potential changes
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun scanForPotentialChanges(
        projectPath: Path,
        projectName: String,
    ): Set<Path> {
        logger.debug { "Сканирование изменений временных меток в: $projectPath" }

        val sourceFiles = getAllSourceFiles(projectPath)
        val projectTimestamp = hashStorage.getSourceSetTimestamp(projectName)

        val changed =
            runBlocking {
                sourceFiles
                    .asFlow()
                    .flowOn(Dispatchers.IO)
                    .flatMapMerge { file ->
                        flow {
                            try {
                                val currentTimestamp = Files.getLastModifiedTime(file).toMillis()

                                when {
                                    projectTimestamp == null -> {
                                        // New file
                                        logger.trace { "Обнаружен новый файл: $file" }
                                        emit(file)
                                    }

                                    currentTimestamp > projectTimestamp -> {
                                        // Potentially modified file
                                        logger.trace {
                                            "Потенциально измененный файл: $file (текущая: $currentTimestamp, сохраненная: $projectTimestamp)"
                                        }
                                        emit(file)
                                    }

                                    else -> false
                                }
                            } catch (e: Exception) {
                                logger.debug(e) { "Ошибка при проверке временной метки для файла: $file" }
                                emit(file) // Include in potential changes if we can't verify
                            }
                        }
                    }.toSet()
            }
        return changed
    }

    /**
     * Phase 2: Hash verification for potential changes with parallel processing
     */
    private fun verifyChangesWithHashes(candidates: Set<Path>): ChangesSet {
        logger.debug { "Проверка ${candidates.size} потенциальных изменений с вычислением хешей" }

        // Process files in batches to avoid overwhelming the system
        val batchSize = maxConcurrentHashCalculations
        val results = mutableMapOf<Path, Pair<ChangeType, String>>()

        candidates.chunked(batchSize).forEach { batch ->
            val batchResults = batch.map(::verifyFileChange)

            // Collect results
            batch.zip(batchResults).forEach { (file, result) ->
                if (result.first != ChangeType.UNCHANGED) {
                    results[file] = result
                }
            }
        }

        logger.debug { "Проверка хешей завершена: обнаружено ${results.size} фактических изменений" }
        return results
    }

    /**
     * Verifies if a single file has actually changed by comparing content hashes
     */
    private fun verifyFileChange(file: Path): Pair<ChangeType, String> {
        try {
            val currentHash = calculateFileHash(file)
            val storedHash = hashStorage.getHash(file)

            val type =
                when {
                    storedHash == null -> {
                        logger.trace { "Новый файл подтвержден: $file" }
                        ChangeType.NEW
                    }

                    currentHash != storedHash -> {
                        logger.trace { "Измененный файл подтвержден: $file" }
                        ChangeType.MODIFIED
                    }

                    else -> {
                        logger.trace { "Файл не изменен: $file" }
                        ChangeType.UNCHANGED
                    }
                }
            return Pair(type, currentHash)
        } catch (e: Exception) {
            logger.debug(e) { "Ошибка при проверке изменения файла: $file" }
            return Pair(ChangeType.MODIFIED, "") // Assume modified if we can't verify
        }
    }

    /**
     * Gets all source files in the project that should be tracked for changes
     */
    private fun getAllSourceFiles(projectPath: Path): Sequence<Path> {
        try {
            if (!Files.exists(projectPath)) {
                logger.warn { "Путь проекта не существует: $projectPath" }
                return emptySequence()
            }

            return projectPath
                .walk()
                .filter { it.isRegularFile() }
                .filter { !isIgnoredPath(it, projectPath) }
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при сканировании исходных файлов в: $projectPath" }
            return emptySequence()
        }
    }

    /**
     * Determines if a path should be ignored (e.g., build outputs, temp files)
     */
    private fun isIgnoredPath(
        path: Path,
        projectRoot: Path,
    ): Boolean {
        val relativePath = projectRoot.relativize(path).toString().replace("\\", "/")

        val ignoredPatterns =
            listOf(
                ".yaxunit/",
                "build/",
                "target/",
                ".git/",
                ".gradle/",
                "temp/",
                "tmp/",
                "ConfigDumpInfo.xml",
            )

        return ignoredPatterns.any { pattern ->
            relativePath.startsWith(pattern) || relativePath.contains("/$pattern")
        }
    }
}
