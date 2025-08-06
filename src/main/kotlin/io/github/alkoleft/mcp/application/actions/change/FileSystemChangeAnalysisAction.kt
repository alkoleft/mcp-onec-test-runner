package io.github.alkoleft.mcp.application.actions.change

import io.github.alkoleft.mcp.application.actions.ChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.ChangeAnalysisResult
import io.github.alkoleft.mcp.application.actions.exceptions.AnalyzeException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.ChangeType
import io.github.alkoleft.mcp.core.modules.FileWatcher
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Реализация ChangeAnalysisAction для анализа изменений в файловой системе
 */
class FileSystemChangeAnalysisAction(
    private val fileWatcher: FileWatcher
) : ChangeAnalysisAction {

    override suspend fun analyze(properties: ApplicationProperties): ChangeAnalysisResult {
        logger.info { "Analyzing changes for project: ${properties.basePath}" }

        return withContext(Dispatchers.IO) {
            try {
                val changedFiles = fileWatcher.getModifiedFiles(properties.basePath)

                if (changedFiles.isEmpty()) {
                    logger.info { "No changes detected in project" }
                    return@withContext ChangeAnalysisResult(hasChanges = false)
                }

                logger.info { "Found ${changedFiles.size} changed files" }

                val affectedModules = determineAffectedModules(changedFiles, properties)
                val changeTypes = determineChangeTypes(changedFiles, properties)

                logger.info { "Affected modules: ${affectedModules.joinToString(", ")}" }

                ChangeAnalysisResult(
                    hasChanges = true,
                    changedFiles = changedFiles,
                    affectedModules = affectedModules,
                    changeTypes = changeTypes
                )

            } catch (e: Exception) {
                logger.error(e) { "Change analysis failed" }
                throw AnalyzeException("Change analysis failed: ${e.message}", e)
            }
        }
    }

    private fun determineAffectedModules(
        changedFiles: Set<Path>,
        properties: ApplicationProperties
    ): Set<String> {
        return properties.sourceSet
            .filter { sourceItem ->
                val sourcePath = properties.basePath.resolve(sourceItem.path)
                changedFiles.any { changedFile ->
                    changedFile.startsWith(sourcePath)
                }
            }
            .map { it.path }
            .toSet()
    }

    private fun determineChangeTypes(
        changedFiles: Set<Path>,
        properties: ApplicationProperties
    ): Map<Path, ChangeType> {
        return changedFiles.associateWith { file ->
            when {
                file.toString().contains("test", ignoreCase = true) -> ChangeType.MODIFIED
                file.toString().contains("config", ignoreCase = true) -> ChangeType.MODIFIED
                else -> ChangeType.MODIFIED
            }
        }
    }
} 