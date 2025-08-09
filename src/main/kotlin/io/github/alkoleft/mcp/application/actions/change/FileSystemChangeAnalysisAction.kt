package io.github.alkoleft.mcp.application.actions.change

import io.github.alkoleft.mcp.application.actions.ChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.ChangeAnalysisResult
import io.github.alkoleft.mcp.application.actions.FileSystemChangeAnalysisResult
import io.github.alkoleft.mcp.application.actions.exceptions.AnalyzeException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.storage.FileBuildStateManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Реализация ChangeAnalysisAction для анализа изменений в файловой системе
 * с использованием Enhanced Hybrid Hash Detection и группировкой по source set
 */
@Component
class FileSystemChangeAnalysisAction(
    private val buildStateManager: FileBuildStateManager,
    private val sourceSetAnalyzer: SourceSetChangeAnalyzer,
) : ChangeAnalysisAction {
    override suspend fun analyze(properties: ApplicationProperties): ChangeAnalysisResult {
        logger.info { "Analyzing changes for project: ${properties.basePath}" }

        return withContext(Dispatchers.IO) {
            try {
                // Use FileBuildStateManager's Enhanced Hybrid Hash Detection
                val changes = buildStateManager.checkChanges(properties.basePath)

                if (changes.isEmpty()) {
                    logger.info { "No changes detected in project" }
                    return@withContext ChangeAnalysisResult(hasChanges = false)
                }

                logger.info { "Found ${changes.size} changed files using Enhanced Hybrid Hash Detection" }

                val changedFiles = changes.keys
                val affectedModules = determineAffectedModules(changedFiles, properties)

                logger.info { "Affected modules: ${affectedModules.joinToString(", ")}" }

                ChangeAnalysisResult(
                    hasChanges = true,
                    changedFiles = changedFiles,
                    affectedModules = affectedModules,
                    changeTypes = changes,
                )
            } catch (e: Exception) {
                logger.error(e) { "Change analysis failed" }
                throw AnalyzeException("Change analysis failed: ${e.message}", e)
            }
        }
    }

    override suspend fun analyzeBySourceSet(properties: ApplicationProperties): FileSystemChangeAnalysisResult {
        logger.info { "Analyzing changes by source set for project: ${properties.basePath}" }

        return withContext(Dispatchers.IO) {
            try {
                // Use FileBuildStateManager's Enhanced Hybrid Hash Detection
                val changes = buildStateManager.checkChanges(properties.basePath)

                if (changes.isEmpty()) {
                    logger.info { "No changes detected in project" }
                    return@withContext FileSystemChangeAnalysisResult(
                        hasChangesFlag = false,
                        changedFilesSet = emptySet(),
                        sourceSetChanges = emptyMap(),
                        changeTypesMap = emptyMap(),
                        analysisTimestamp = Instant.now(),
                    )
                }

                logger.info { "Found ${changes.size} changed files, analyzing by source set" }

                // Group changes by source set
                val sourceSetChanges = sourceSetAnalyzer.analyzeSourceSetChanges(properties, changes)

                logger.info { "Changes grouped into ${sourceSetChanges.size} affected source sets" }

                FileSystemChangeAnalysisResult(
                    hasChangesFlag = true,
                    changedFilesSet = changes.keys,
                    sourceSetChanges = sourceSetChanges,
                    changeTypesMap = changes,
                    analysisTimestamp = Instant.now(),
                )
            } catch (e: Exception) {
                logger.error(e) { "Source set change analysis failed" }
                throw AnalyzeException("Source set change analysis failed: ${e.message}", e)
            }
        }
    }

    override suspend fun saveSourceSetState(
        properties: ApplicationProperties,
        sourceSetChanges: SourceSetChanges,
    ): Boolean {
        logger.debug { "Saving source set state for: ${sourceSetChanges.sourceSetName} in project: ${properties.basePath}" }

        return withContext(Dispatchers.IO) {
            try {
                if (sourceSetChanges.changedFiles.isNotEmpty()) {
                    // Calculate and store hashes for changed files in this source set
                    val hashUpdates = sourceSetChanges.changeTypes.entries.associate { it.key to it.value.second }

                    buildStateManager.updateHashes(hashUpdates)
                    logger.info { "Updated ${hashUpdates.size} file hashes for source set: ${sourceSetChanges.sourceSetName}" }
                }

                // Update build timestamp
                buildStateManager.setLastBuildTime(properties.basePath, System.currentTimeMillis())

                true
            } catch (e: Exception) {
                logger.error(e) { "Failed to save source set state for: ${sourceSetChanges.sourceSetName}" }
                false
            }
        }
    }

    private fun determineAffectedModules(
        changedFiles: Set<Path>,
        properties: ApplicationProperties,
    ): Set<String> =
        properties.sourceSet
            .filter { sourceItem ->
                val sourcePath = properties.basePath.resolve(sourceItem.path)
                changedFiles.any { changedFile ->
                    try {
                        changedFile.startsWith(sourcePath)
                    } catch (e: Exception) {
                        logger.debug(e) { "Error checking if file $changedFile belongs to source set ${sourceItem.path}" }
                        false
                    }
                }
            }.map { it.path }
            .toSet()
}
