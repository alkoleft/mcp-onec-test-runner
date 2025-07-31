package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.core.modules.BuildDecision
import io.github.alkoleft.mcp.core.modules.BuildResult
import io.github.alkoleft.mcp.core.modules.BuildService
import io.github.alkoleft.mcp.core.modules.BuildStateManager
import io.github.alkoleft.mcp.core.modules.BuildType
import io.github.alkoleft.mcp.core.modules.ChangeType
import io.github.alkoleft.mcp.core.modules.UtilLocator
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

private val logger = KotlinLogging.logger {  }

/**
 * Build orchestration service implementing the Enhanced Hybrid Hash Detection algorithm.
 * Manages full and incremental builds based on file change detection.
 */
@Service
class BuildOrchestrationService(
    private val buildStateManager: BuildStateManager,
    private val utilLocator: UtilLocator,
) : BuildService {

    // Build decision thresholds
    private val incrementalThreshold = 10 // Max modules for incremental build
    private val skipThresholdMinutes = 5L // Skip builds if recent successful build

    override suspend fun ensureBuild(projectPath: Path): BuildResult {
        logger.info { "Ensuring build is up to date for project: $projectPath" }

        val buildDecision = determineBuildStrategy(projectPath)

        return when (buildDecision) {
            is BuildDecision.NO_BUILD_NEEDED -> {
                logger.info { "No build needed - project is up to date" }
                BuildResult(
                    success = true,
                    duration = Duration.ZERO,
                    buildType = BuildType.SKIP,
                )
            }

            is BuildDecision.FULL_BUILD -> {
                logger.info { "Performing full build" }
                performFullBuild(projectPath)
            }

            is BuildDecision.INCREMENTAL_BUILD -> {
                logger.info { "Performing incremental build for modules: ${buildDecision.changedModules}" }
                performIncrementalBuild(projectPath, buildDecision.changedModules)
            }

            is BuildDecision.INCREMENTAL_TESTS -> {
                logger.info { "Only test files changed - minimal build required" }
                performTestOnlyBuild(projectPath)
            }
        }
    }

    override suspend fun determineBuildStrategy(projectPath: Path): BuildDecision =
        coroutineScope {
            val startTime = Instant.now()

            try {
                // Phase 1: Check if recent successful build exists
                val lastBuildTime = buildStateManager.getLastBuildTime(projectPath)
                val currentTime = System.currentTimeMillis()

                if (lastBuildTime != null &&
                    (currentTime - lastBuildTime) < Duration.ofMinutes(skipThresholdMinutes).toMillis()
                ) {
                    logger.debug("Recent build found, checking for changes since then")
                }

                // Phase 2: Enhanced Hybrid Algorithm - Fast timestamp pre-scan + hash verification
                val changes = buildStateManager.checkChanges(projectPath)

                if (changes.isEmpty()) {
                    return@coroutineScope BuildDecision.NO_BUILD_NEEDED
                }

                // Phase 3: Analyze change impact
                val changeAnalysis = analyzeChangeImpact(projectPath, changes)

                // Phase 4: Build decision logic
                val decision =
                    when {
                        changeAnalysis.coreFilesChanged -> {
                            logger.debug("Core files changed - full build required")
                            BuildDecision.FULL_BUILD
                        }

                        changeAnalysis.configurationChanged -> {
                            logger.debug("Configuration files changed - full build required")
                            BuildDecision.FULL_BUILD
                        }

                        changeAnalysis.testOnlyChanges -> {
                            logger.debug("Only test files changed - incremental test build")
                            BuildDecision.INCREMENTAL_TESTS
                        }

                        changeAnalysis.changedModules.size > incrementalThreshold -> {
                            logger.debug("Too many modules changed (${changeAnalysis.changedModules.size}) - full build")
                            BuildDecision.FULL_BUILD
                        }

                        else -> {
                            logger.debug("Incremental build for ${changeAnalysis.changedModules.size} modules")
                            BuildDecision.INCREMENTAL_BUILD(changeAnalysis.changedModules)
                        }
                    }

                val analysisTime = Duration.between(startTime, Instant.now())
                logger.debug("Build strategy determined in ${analysisTime.toMillis()}ms: $decision")

                decision
            } catch (e: Exception) {
                logger.warn("Error determining build strategy, falling back to full build", e)
                BuildDecision.FULL_BUILD
            }
        }

    override suspend fun performFullBuild(projectPath: Path): BuildResult {
        val startTime = Instant.now()

        return try {
            logger.info { "Starting full build for project: $projectPath" }

            // Locate 1C compiler
            val utilityLocation = utilLocator.locateUtility(UtilityType.DESIGNER)

            // Execute full compilation
            val buildSuccess = executeBuild(projectPath, utilityLocation, BuildType.FULL)

            if (buildSuccess) {
                // Update hashes for all source files
                updateAllFileHashes(projectPath)

                // Update last build time
                buildStateManager.setLastBuildTime(projectPath, System.currentTimeMillis())

                val duration = Duration.between(startTime, Instant.now())
                logger.info { "Full build completed successfully in ${duration.toMillis()}ms" }

                BuildResult(
                    success = true,
                    duration = duration,
                    buildType = BuildType.FULL,
                )
            } else {
                val duration = Duration.between(startTime, Instant.now())
                BuildResult(
                    success = false,
                    duration = duration,
                    buildType = BuildType.FULL,
                    error = "Full build failed - check 1C compiler output",
                )
            }
        } catch (e: Exception) {
            logger.error("Full build failed with exception", e)
            BuildResult(
                success = false,
                duration = Duration.between(startTime, Instant.now()),
                buildType = BuildType.FULL,
                error = "Full build failed: ${e.message}",
            )
        }
    }

    override suspend fun performIncrementalBuild(
        projectPath: Path,
        changedModules: Set<String>,
    ): BuildResult {
        val startTime = Instant.now()

        return try {
            logger.info { "Starting incremental build for modules: $changedModules" }

            // Locate 1C compiler
            val utilityLocation = utilLocator.locateUtility(UtilityType.DESIGNER)

            // Execute incremental compilation
            val buildSuccess = executeBuild(projectPath, utilityLocation, BuildType.INCREMENTAL, changedModules)

            if (buildSuccess) {
                // Update hashes only for changed files
                updateChangedFileHashes(projectPath, changedModules)

                // Update last build time
                buildStateManager.setLastBuildTime(projectPath, System.currentTimeMillis())

                val duration = Duration.between(startTime, Instant.now())
                logger.info { "Incremental build completed successfully in ${duration.toMillis()}ms" }

                BuildResult(
                    success = true,
                    duration = duration,
                    buildType = BuildType.INCREMENTAL,
                )
            } else {
                logger.warn("Incremental build failed, falling back to full build")
                performFullBuild(projectPath)
            }
        } catch (e: Exception) {
            logger.error("Incremental build failed, falling back to full build", e)
            performFullBuild(projectPath)
        }
    }

    /**
     * Performs test-only build when only test files have changed
     */
    private suspend fun performTestOnlyBuild(projectPath: Path): BuildResult {
        val startTime = Instant.now()

        return try {
            logger.info { "Performing test-only build" }

            // For test-only changes, we might not need full compilation
            // Just ensure test environment is ready

            val duration = Duration.between(startTime, Instant.now())
            logger.info { "Test-only build completed in ${duration.toMillis()}ms" }

            BuildResult(
                success = true,
                duration = duration,
                buildType = BuildType.SKIP,
            )
        } catch (e: Exception) {
            logger.error(e) { "Test-only build failed" }
            BuildResult(
                success = false,
                duration = Duration.between(startTime, Instant.now()),
                buildType = BuildType.SKIP,
                error = "Test-only build failed: ${e.message}",
            )
        }
    }

    /**
     * Analyzes the impact of file changes to determine build strategy
     */
    private suspend fun analyzeChangeImpact(
        projectPath: Path,
        changes: Map<Path, ChangeType>,
    ): ChangeAnalysis =
        withContext(Dispatchers.IO) {
            val coreFilePatterns = setOf(".bsl", ".os", ".cf", ".epf")
            val configFilePatterns = setOf("configuration.xml", "configDumpInfo.xml")
            val testFilePatterns = setOf("Test", "Тест")

            var coreFilesChanged = false
            var configurationChanged = false
            var testOnlyChanges = true
            val changedModules = mutableSetOf<String>()

            for ((path, changeType) in changes) {
                if (changeType == ChangeType.UNCHANGED) continue

                val fileName = path.fileName.toString()
                val fileExtension = path.toString().substringAfterLast(".", "")

                // Check if it's a core file
                if (coreFilePatterns.contains(".$fileExtension")) {
                    coreFilesChanged = true
                    testOnlyChanges = false

                    // Extract module name from path
                    val moduleName = extractModuleName(projectPath, path)
                    if (moduleName != null) {
                        changedModules.add(moduleName)
                    }
                }

                // Check if it's a configuration file
                if (configFilePatterns.any { fileName.contains(it) }) {
                    configurationChanged = true
                    testOnlyChanges = false
                }

                // Check if it's NOT a test file
                if (!testFilePatterns.any { fileName.contains(it, ignoreCase = true) }) {
                    testOnlyChanges = false
                }
            }

            ChangeAnalysis(
                coreFilesChanged = coreFilesChanged,
                configurationChanged = configurationChanged,
                testOnlyChanges = testOnlyChanges && !coreFilesChanged && !configurationChanged,
                changedModules = changedModules,
            )
        }

    /**
     * Executes the actual build process using 1C compiler
     */
    private suspend fun executeBuild(
        projectPath: Path,
        utilityLocation: UtilityLocation,
        buildType: BuildType,
        changedModules: Set<String>? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            // This is a placeholder for actual 1C compilation
            // In real implementation, this would:
            // 1. Create temporary configuration file
            // 2. Execute 1cv8c DESIGNER with appropriate parameters
            // 3. Monitor compilation process
            // 4. Return success/failure based on exit code

            logger.debug("Executing $buildType build with utility: ${utilityLocation.executablePath}")

            // Simulate build time based on build type
            val buildTime =
                when (buildType) {
                    BuildType.FULL -> 5000L // 5 seconds for full build
                    BuildType.INCREMENTAL -> 2000L // 2 seconds for incremental
                    BuildType.SKIP -> 100L // 100ms for skip
                }

            Thread.sleep(buildTime)

            // For now, return success (in real implementation, check actual process result)
            true
        }

    /**
     * Updates file hashes for all source files after successful build
     */
    private suspend fun updateAllFileHashes(projectPath: Path) =
        withContext(Dispatchers.IO) {
            val sourceFiles =
                projectPath
                    .walk()
                    .filter { it.isRegularFile() }
                    .filter { isSourceFile(it) }
                    .toList()

            val hashes =
                sourceFiles.associate { file ->
                    file to calculateFileHash(file)
                }

            buildStateManager.updateHashes(projectPath, hashes)
            logger.debug("Updated hashes for ${hashes.size} source files")
        }

    /**
     * Updates file hashes only for changed modules
     */
    private suspend fun updateChangedFileHashes(
        projectPath: Path,
        changedModules: Set<String>,
    ) = withContext(Dispatchers.IO) {
        // Find files belonging to changed modules
        val changedFiles =
            projectPath
                .walk()
                .filter { it.isRegularFile() }
                .filter { file ->
                    val moduleName = extractModuleName(projectPath, file)
                    moduleName != null && changedModules.contains(moduleName)
                }.toList()

        val hashes =
            changedFiles.associate { file ->
                file to calculateFileHash(file)
            }

        buildStateManager.updateHashes(projectPath, hashes)
        logger.debug("Updated hashes for ${hashes.size} files in changed modules")
    }

    /**
     * Checks if a file is a source file that should be tracked
     */
    private fun isSourceFile(path: Path): Boolean {
        val extension = path.toString().substringAfterLast(".", "").lowercase()
        return extension in setOf("bsl", "os", "cf", "epf", "xml")
    }

    /**
     * Extracts module name from file path
     */
    private fun extractModuleName(
        projectPath: Path,
        filePath: Path,
    ): String? {
        val relativePath = projectPath.relativize(filePath)
        val pathParts = relativePath.toString().split(System.getProperty("file.separator"))

        // Return the first directory as module name
        return if (pathParts.size > 1) pathParts[0] else null
    }

    /**
     * Calculates SHA-256 hash of file content
     */
    private suspend fun calculateFileHash(file: Path): String =
        withContext(Dispatchers.IO) {
            val bytes = Files.readAllBytes(file)
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(bytes)
            hashBytes.joinToString("") { "%02x".format(it) }
        }

    /**
     * Analysis result of file changes
     */
    private data class ChangeAnalysis(
        val coreFilesChanged: Boolean,
        val configurationChanged: Boolean,
        val testOnlyChanges: Boolean,
        val changedModules: Set<String>,
    )
}
