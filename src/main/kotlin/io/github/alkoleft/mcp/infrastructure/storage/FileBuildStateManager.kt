package io.github.alkoleft.mcp.infrastructure.storage

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.BuildStateManager
import io.github.alkoleft.mcp.core.modules.ChangeType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
) : BuildStateManager {
    // Performance tuning parameters
    private val maxConcurrentHashCalculations = 4

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun checkChanges(properties: ApplicationProperties): ChangesSet {
        val startTime = Instant.now()
        logger.debug { "Запуск Enhanced Hybrid Hash Detection для проекта: ${properties.basePath}" }

        try {
            // Phase 1: Fast timestamp pre-scan
            val candidateFiles =
                properties.sourceSet
                    .map { properties.basePath.resolve(it.path) }
                    .flatMap { scanForPotentialChanges(it) }
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

    override fun updateHashes(files: Map<Path, String>) {
        logger.debug { "Обновление хешей для ${files.size} файлов" }

        try {
            hashStorage.batchUpdate(files)
            logger.debug { "Хеши ${files.size} файлов успешно обновлены" }
        } catch (e: Exception) {
            logger.error(e) { "Не удалось обновить хеши файлов" }
            throw e
        }
    }

    /**
     * Phase 1: Fast timestamp pre-scan to identify potential changes
     */
    private fun scanForPotentialChanges(projectPath: Path): Set<Path> {
        logger.debug { "Сканирование изменений временных меток в: $projectPath" }

        val potentialChanges = mutableSetOf<Path>()
        val sourceFiles = getAllSourceFiles(projectPath)

        for (file in sourceFiles) {
            try {
                val currentTimestamp = Files.getLastModifiedTime(file).toMillis()
                val storedTimestamp = hashStorage.getTimestamp(file)

                when {
                    storedTimestamp == null -> {
                        // New file
                        potentialChanges.add(file)
                        logger.trace { "Обнаружен новый файл: $file" }
                    }

                    currentTimestamp > storedTimestamp -> {
                        // Potentially modified file
                        potentialChanges.add(file)
                        logger.trace { "Потенциально измененный файл: $file (текущая: $currentTimestamp, сохраненная: $storedTimestamp)" }
                    }
                }
            } catch (e: Exception) {
                logger.debug(e) { "Ошибка при проверке временной метки для файла: $file" }
                potentialChanges.add(file) // Include in potential changes if we can't verify
            }
        }

        return potentialChanges
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
    private fun getAllSourceFiles(projectPath: Path): List<Path> {
        try {
            if (!Files.exists(projectPath)) {
                logger.warn { "Путь проекта не существует: $projectPath" }
                return emptyList()
            }

            return projectPath
                .walk()
                .filter { it.isRegularFile() }
                .filter { !isIgnoredPath(it, projectPath) }
                .toList()
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при сканировании исходных файлов в: $projectPath" }
            return emptyList()
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
