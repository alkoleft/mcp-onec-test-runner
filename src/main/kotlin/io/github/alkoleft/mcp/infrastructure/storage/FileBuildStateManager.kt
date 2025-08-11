package io.github.alkoleft.mcp.infrastructure.storage

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.BuildStateManager
import io.github.alkoleft.mcp.core.modules.ChangeType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.withContext
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
    override suspend fun checkChanges(properties: ApplicationProperties): ChangesSet =
        coroutineScope {
            val startTime = Instant.now()
            logger.debug { "Starting Enhanced Hybrid Hash Detection for project: ${properties.basePath}" }

            try {
                // Phase 1: Fast timestamp pre-scan
                val candidateFiles = properties.sourceSet.asFlow()
                    .map { properties.basePath.resolve(it.path) }
                    .flatMapMerge { scanForPotentialChanges(it).asFlow() }
                    .toSet()

                logger.debug { "Phase 1: Found ${candidateFiles.size} potential changes after timestamp scan" }

                if (candidateFiles.isEmpty()) {
                    logger.debug { "No potential changes detected - skipping hash verification" }
                    return@coroutineScope emptyMap()
                }

                // Phase 2: Hash verification for potential changes
                val actualChanges = verifyChangesWithHashes(candidateFiles)

                val duration = java.time.Duration.between(startTime, Instant.now())
                logger.info {
                    "Change detection completed in ${duration.toMillis()}ms: ${actualChanges.size} actual changes from ${candidateFiles.size} candidates"
                }

                actualChanges
            } catch (e: Exception) {
                logger.error(e) { "Error during change detection" }
                // Fallback: treat all source files as changed
                getAllSourceFiles(properties.basePath).associateWith { Pair(ChangeType.MODIFIED, "") }
            }
        }

    override suspend fun updateHashes(files: Map<Path, String>) {
        logger.debug { "Updating hashes for ${files.size} files" }

        try {
            hashStorage.batchUpdate(files)
            logger.debug { "Successfully updated ${files.size} file hashes" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to update file hashes" }
            throw e
        }
    }

    /**
     * Phase 1: Fast timestamp pre-scan to identify potential changes
     */
    private suspend fun scanForPotentialChanges(projectPath: Path): Set<Path> =
        withContext(Dispatchers.IO) {
            logger.debug { "Scanning for timestamp changes in: $projectPath" }

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
                            logger.trace { "New file detected: $file" }
                        }

                        currentTimestamp > storedTimestamp -> {
                            // Potentially modified file
                            potentialChanges.add(file)
                            logger.trace { "Potentially modified file: $file (current: $currentTimestamp, stored: $storedTimestamp)" }
                        }
                    }
                } catch (e: Exception) {
                    logger.debug(e) { "Error checking timestamp for file: $file" }
                    potentialChanges.add(file) // Include in potential changes if we can't verify
                }
            }

            potentialChanges
        }

    /**
     * Phase 2: Hash verification for potential changes with parallel processing
     */
    private suspend fun verifyChangesWithHashes(candidates: Set<Path>): ChangesSet =
        coroutineScope {
            logger.debug { "Verifying ${candidates.size} potential changes with hash calculation" }

            // Process files in batches to avoid overwhelming the system
            val batchSize = maxConcurrentHashCalculations
            val results = mutableMapOf<Path, Pair<ChangeType, String>>()

            candidates.chunked(batchSize).forEach { batch ->
                val batchResults =
                    batch
                        .map { file ->
                            async(Dispatchers.IO) {
                                verifyFileChange(file)
                            }
                        }.awaitAll()

                // Collect results
                batch.zip(batchResults).forEach { (file, result) ->
                    if (result.first != ChangeType.UNCHANGED) {
                        results[file] = result
                    }
                }
            }

            logger.debug { "Hash verification completed: ${results.size} actual changes detected" }
            results
        }

    /**
     * Verifies if a single file has actually changed by comparing content hashes
     */
    private suspend fun verifyFileChange(file: Path): Pair<ChangeType, String> =
        withContext(Dispatchers.IO) {
            try {
                val currentHash = calculateFileHash(file)
                val storedHash = hashStorage.getHash(file)

                val type =
                    when {
                        storedHash == null -> {
                            logger.trace { "New file confirmed: $file" }
                            ChangeType.NEW
                        }

                        currentHash != storedHash -> {
                            logger.trace { "Modified file confirmed: $file" }
                            ChangeType.MODIFIED
                        }

                        else -> {
                            logger.trace { "File unchanged: $file" }
                            ChangeType.UNCHANGED
                        }
                    }
                return@withContext Pair(type, currentHash)
            } catch (e: Exception) {
                logger.debug(e) { "Error verifying file change: $file" }
                Pair(ChangeType.MODIFIED, "") // Assume modified if we can't verify
            }
        }

    /**
     * Gets all source files in the project that should be tracked for changes
     */
    private suspend fun getAllSourceFiles(projectPath: Path): List<Path> =
        withContext(Dispatchers.IO) {
            try {
                if (!Files.exists(projectPath)) {
                    logger.warn { "Project path does not exist: $projectPath" }
                    return@withContext emptyList()
                }

                projectPath
                    .walk()
                    .filter { it.isRegularFile() }
                    .filter { !isIgnoredPath(it, projectPath) }
                    .toList()
            } catch (e: Exception) {
                logger.error(e) { "Error scanning source files in: $projectPath" }
                emptyList()
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
