package io.github.alkoleft.mcp.infrastructure.storage

import io.github.alkoleft.mcp.core.modules.BuildStateManager
import io.github.alkoleft.mcp.core.modules.ChangeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

/**
 * Build state manager implementing Enhanced Hybrid Hash Detection algorithm.
 * Combines fast timestamp pre-filtering with accurate hash verification for optimal performance.
 */
private val logger = KotlinLogging.logger {  }

@Component
class FileBuildStateManager(
    private val hashStorage: MapDbHashStorage,
) : BuildStateManager {

    // Supported source file extensions for 1C:Enterprise
    private val sourceFileExtensions = setOf("bsl", "os", "cf", "epf", "xml", "mdo")

    // Performance tuning parameters
    private val maxConcurrentHashCalculations = 4
    private val hashCacheSize = 1000

    override suspend fun checkChanges(projectPath: Path): Map<Path, ChangeType> =
        coroutineScope {
            val startTime = Instant.now()
            logger.debug { "Starting Enhanced Hybrid Hash Detection for project: $projectPath" }

            try {
                // Phase 1: Fast timestamp pre-scan
                val candidateFiles = scanForPotentialChanges(projectPath)
                logger.debug { "Phase 1: Found ${candidateFiles.size} potential changes after timestamp scan" }

                if (candidateFiles.isEmpty()) {
                    logger.debug { "No potential changes detected - skipping hash verification" }
                    return@coroutineScope emptyMap()
                }

                // Phase 2: Hash verification for potential changes
                val actualChanges = verifyChangesWithHashes(candidateFiles)

                val duration = java.time.Duration.between(startTime, Instant.now())
                logger.info { "Change detection completed in ${duration.toMillis()}ms: ${actualChanges.size} actual changes from ${candidateFiles.size} candidates" }

                actualChanges
            } catch (e: Exception) {
                logger.error(e) { "Error during change detection" }
                // Fallback: treat all source files as changed
                getAllSourceFiles(projectPath).associateWith { ChangeType.MODIFIED }
            }
        }

    override suspend fun updateHashes(
        projectPath: Path,
        files: Map<Path, String>,
    ) {
        logger.debug { "Updating hashes for ${files.size} files" }

        try {
            hashStorage.batchUpdate(files)
            logger.debug { "Successfully updated ${files.size} file hashes" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to update file hashes" }
            throw e
        }
    }

    override suspend fun getLastBuildTime(projectPath: Path): Long? =
        try {
            val buildMarkerFile = projectPath.resolve(".yaxunit/last-build-time")
            if (Files.exists(buildMarkerFile)) {
                Files.readString(buildMarkerFile).trim().toLongOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to get last build time for $projectPath" }
            null
        }

    override suspend fun setLastBuildTime(
        projectPath: Path,
        timestamp: Long,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val buildMarkerFile = projectPath.resolve(".yaxunit/last-build-time")
                Files.createDirectories(buildMarkerFile.parent)
                Files.writeString(buildMarkerFile, timestamp.toString())
                logger.debug { "Updated last build time to $timestamp for project: $projectPath" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to set last build time for $projectPath" }
                throw e
            }
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
    private suspend fun verifyChangesWithHashes(candidates: Set<Path>): Map<Path, ChangeType> =
        coroutineScope {
            logger.debug { "Verifying ${candidates.size} potential changes with hash calculation" }

            // Process files in batches to avoid overwhelming the system
            val batchSize = maxConcurrentHashCalculations
            val results = mutableMapOf<Path, ChangeType>()

            candidates.chunked(batchSize).forEach { batch ->
                val batchResults =
                    batch
                        .map { file ->
                            async(Dispatchers.IO) {
                                verifyFileChange(file)
                            }
                        }.awaitAll()

                // Collect results
                batch.zip(batchResults).forEach { (file, changeType) ->
                    if (changeType != ChangeType.UNCHANGED) {
                        results[file] = changeType
                    }
                }
            }

            logger.debug { "Hash verification completed: ${results.size} actual changes detected" }
            results
        }

    /**
     * Verifies if a single file has actually changed by comparing content hashes
     */
    private suspend fun verifyFileChange(file: Path): ChangeType =
        withContext(Dispatchers.IO) {
            try {
                val currentHash = calculateFileHash(file)
                val storedHash = hashStorage.getHash(file)

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
            } catch (e: Exception) {
                logger.debug(e) { "Error verifying file change: $file" }
                ChangeType.MODIFIED // Assume modified if we can't verify
            }
        }

    /**
     * Calculates SHA-256 hash of file content with optimized buffering
     */
    private suspend fun calculateFileHash(file: Path): String =
        withContext(Dispatchers.IO) {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192) // 8KB buffer for optimal I/O performance

                Files.newInputStream(file).use { inputStream ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }

                digest.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                logger.debug(e) { "Failed to calculate hash for file: $file" }
                throw e
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
                    .filter { isSourceFile(it) }
                    .filter { !isIgnoredPath(it, projectPath) }
                    .toList()
            } catch (e: Exception) {
                logger.error(e) { "Error scanning source files in: $projectPath" }
                emptyList()
            }
        }

    /**
     * Determines if a file is a source file that should be tracked
     */
    private fun isSourceFile(path: Path): Boolean {
        val extension = path.toString().substringAfterLast(".", "").lowercase()
        return extension in sourceFileExtensions
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
            )

        return ignoredPatterns.any { pattern ->
            relativePath.startsWith(pattern) || relativePath.contains("/$pattern")
        }
    }

    /**
     * Utility method to get change statistics
     */
    suspend fun getChangeStatistics(projectPath: Path): ChangeStatistics =
        coroutineScope {
            try {
                val allFiles = getAllSourceFiles(projectPath)
                val changes = checkChanges(projectPath)

                ChangeStatistics(
                    totalSourceFiles = allFiles.size,
                    newFiles = changes.count { it.value == ChangeType.NEW },
                    modifiedFiles = changes.count { it.value == ChangeType.MODIFIED },
                    deletedFiles = changes.count { it.value == ChangeType.DELETED },
                    unchangedFiles = allFiles.size - changes.size,
                )
            } catch (e: Exception) {
                logger.error(e) { "Error calculating change statistics" }
                ChangeStatistics(0, 0, 0, 0, 0)
            }
        }
}

/**
 * Statistics about project changes
 */
data class ChangeStatistics(
    val totalSourceFiles: Int,
    val newFiles: Int,
    val modifiedFiles: Int,
    val deletedFiles: Int,
    val unchangedFiles: Int,
) {
    val totalChanges: Int get() = newFiles + modifiedFiles + deletedFiles
    val changePercentage: Double get() = if (totalSourceFiles > 0) (totalChanges.toDouble() / totalSourceFiles) * 100 else 0.0
}
