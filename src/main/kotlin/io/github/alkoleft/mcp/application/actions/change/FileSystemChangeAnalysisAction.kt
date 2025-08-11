package io.github.alkoleft.mcp.application.actions.change

import io.github.alkoleft.mcp.application.actions.ChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.ChangeAnalysisResult
import io.github.alkoleft.mcp.application.actions.exceptions.AnalyzeException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.storage.FileBuildStateManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
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
    override suspend fun run(properties: ApplicationProperties): ChangeAnalysisResult {
        logger.info { "Analyzing changes by source set for project: ${properties.basePath}" }

        return withContext(Dispatchers.IO) {
            try {
                // Use FileBuildStateManager's Enhanced Hybrid Hash Detection
                val changes = buildStateManager.checkChanges(properties.basePath)

                if (changes.isEmpty()) {
                    logger.info { "No changes detected in project" }
                    return@withContext ChangeAnalysisResult(
                        hasChanges = false,
                        changedFiles = emptySet(),
                        changeTypes = emptyMap(),
                        sourceSetChanges = emptyMap(),
                        analysisTimestamp = Instant.now(),
                    )
                }

                logger.info { "Found ${changes.size} changed files, analyzing by source set" }

                // Group changes by source set
                val sourceSetChanges = sourceSetAnalyzer.analyzeSourceSetChanges(properties, changes)

                logger.info { "Changes grouped into ${sourceSetChanges.size} affected source sets" }

                ChangeAnalysisResult(
                    hasChanges = true,
                    changedFiles = changes.keys,
                    changeTypes = changes,
                    sourceSetChanges = sourceSetChanges,
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
}
