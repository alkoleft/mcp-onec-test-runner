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
import io.github.alkoleft.mcp.infrastructure.changes.Scanner
import io.github.alkoleft.mcp.infrastructure.changes.isIgnoredPath
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

private val logger = KotlinLogging.logger { }

/**
 * Build state manager implementing Enhanced Hybrid Hash Detection algorithm.
 * Combines fast timestamp pre-filtering with accurate hash verification for optimal performance.
 */
class FileBuildStateManager(
    private val sourceSetContext: SourceSetContext,
    private val scanner: Scanner,
) {
    private val hashStorage: HashStorage
        get() = sourceSetContext.hashStorage

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun checkChanges(): ChangesSet {
        val startTime = Instant.now()
        logger.debug { "Анализ изменений: ${sourceSetContext.basePath}" }

        try {
            // Phase 1: Fast timestamp pre-scan
            val candidateFiles =
                sourceSetContext.sourceSet
                    .asFlow()
                    .flowOn(Dispatchers.IO)
                    .flatMapMerge {
                        scanner
                            .scanByLastModifiedTime(
                                sourceSetContext.basePath.resolve(it.path),
                                hashStorage.getSourceSetTimestamp(),
                            ).asFlow()
                    }

            // Phase 2: Hash verification for potential changes
            val actualChanges = verifyChangesWithHashes(candidateFiles)

            val duration = java.time.Duration.between(startTime, Instant.now())
            logger.info {
                "Обнаружение изменений завершено за ${duration.toMillis()}мс: ${actualChanges.size} фактических изменений"
            }

            return actualChanges
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при обнаружении изменений" }
            // Fallback: treat all source files as changed
            return getAllSourceFiles(sourceSetContext.basePath).associateWith { Pair(ChangeType.MODIFIED, "") }
        }
    }

    fun updateHashes(files: Map<Path, String>) {
        sourceSetContext.hashStorage.batchUpdate(files)
    }

    fun storeTimestamp(timeStamp: Long) {
        sourceSetContext.hashStorage.storeTimestamp(timeStamp)
    }

    /**
     * Phase 2: Hash verification for potential changes with parallel processing
     */
    private suspend fun verifyChangesWithHashes(candidates: Flow<Path>): ChangesSet {
        logger.debug { "Проверка потенциальных изменений с вычислением хешей" }

        val results = mutableMapOf<Path, Pair<ChangeType, String>>()

        candidates.collect { file ->
            val result = verifyFileChange(file)

            if (result.first != ChangeType.UNCHANGED) {
                results[file] = result
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

}
