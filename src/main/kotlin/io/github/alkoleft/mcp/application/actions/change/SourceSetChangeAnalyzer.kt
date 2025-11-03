package io.github.alkoleft.mcp.application.actions.change

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.ChangeType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

typealias ChangesSet = Map<Path, Pair<ChangeType, String>>

/**
 * Analyzes and groups file changes by source set for targeted build optimization.
 * This component works with FileBuildStateManager to provide source set-specific change analysis.
 */
@Component
class SourceSetChangeAnalyzer {
    /**
     * Analyzes all changes and groups them by source set with detailed change type information
     */
    fun analyzeSourceSetChanges(
        properties: ApplicationProperties,
        allChanges: ChangesSet,
    ): Map<String, SourceSetChanges> {
        logger.debug { "Анализ ${allChanges.size} изменений для группировки по source set" }

        if (allChanges.isEmpty()) {
            logger.debug { "Нет изменений для анализа" }
            return emptyMap()
        }

        val sourceSetChanges = mutableMapOf<String, SourceSetChanges>()

        properties.sourceSet.forEach { sourceItem ->
            val sourceSetPath = properties.basePath.resolve(sourceItem.path)

            // Find changes that belong to this source set
            val sourceSetFileChanges =
                allChanges.filterKeys { changedFile ->
                    try {
                        changedFile.startsWith(sourceSetPath)
                    } catch (e: Exception) {
                        logger.debug(e) { "Ошибка при проверке принадлежности файла $changedFile к source set ${sourceItem.name}" }
                        false
                    }
                }

            if (sourceSetFileChanges.isNotEmpty()) {
                sourceSetChanges[sourceItem.name] =
                    SourceSetChanges(
                        sourceSetName = sourceItem.name,
                        sourceSetPath = sourceSetPath.toString(),
                        changedFiles = sourceSetFileChanges.keys,
                        changeTypes = sourceSetFileChanges,
                        hasChanges = true,
                    )

                val changeTypeSummary = sourceSetFileChanges.values.groupingBy { it.first }.eachCount()
                logger.debug {
                    "Source set '${sourceItem.name}': ${sourceSetFileChanges.size} changes " +
                        "(${changeTypeSummary[ChangeType.NEW] ?: 0} new, " +
                        "${changeTypeSummary[ChangeType.MODIFIED] ?: 0} modified, " +
                        "${changeTypeSummary[ChangeType.DELETED] ?: 0} deleted)"
                }
            }
        }

        logger.info {
            "Проанализированы изменения для ${sourceSetChanges.size} затронутых source sets из ${properties.sourceSet.size} всего"
        }
        return sourceSetChanges
    }
}

/**
 * Represents changes within a specific source set
 */
data class SourceSetChanges(
    val sourceSetName: String,
    val sourceSetPath: String,
    val changedFiles: Set<Path>,
    val changeTypes: ChangesSet,
    val hasChanges: Boolean,
)
