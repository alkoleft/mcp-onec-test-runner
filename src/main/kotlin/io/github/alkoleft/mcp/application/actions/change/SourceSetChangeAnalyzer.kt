package io.github.alkoleft.mcp.application.actions.change

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ChangeType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     * Groups changed files by their corresponding source sets
     */
    suspend fun groupChangesBySourceSet(
        changedFiles: Set<Path>,
        sourceSet: SourceSet,
        basePath: Path,
    ): Map<String, SourceSetChanges> =
        withContext(Dispatchers.Default) {
            logger.debug { "Grouping ${changedFiles.size} changed files by source set" }

            val sourceSetChanges = mutableMapOf<String, SourceSetChanges>()

            sourceSet.forEach { sourceItem ->
                val sourceSetPath = basePath.resolve(sourceItem.path)
                val sourceSetName = sourceItem.path

                // Find files that belong to this source set
                val sourceSetFiles =
                    changedFiles
                        .filter { changedFile ->
                            try {
                                changedFile.startsWith(sourceSetPath)
                            } catch (e: Exception) {
                                logger.debug(e) { "Error checking if file $changedFile belongs to source set $sourceSetName" }
                                false
                            }
                        }.toSet()

                if (sourceSetFiles.isNotEmpty()) {
                    sourceSetChanges[sourceSetName] =
                        SourceSetChanges(
                            sourceSetName = sourceSetName,
                            sourceSetPath = sourceSetPath.toString(),
                            changedFiles = sourceSetFiles,
                            changeTypes =
                                sourceSetFiles.associateWith {
                                    Pair(ChangeType.MODIFIED, "")
                                },
                            // Will be filled by caller
                            hasChanges = true,
                        )
                    logger.debug { "Source set '$sourceSetName' has ${sourceSetFiles.size} changed files" }
                }
            }

            logger.info { "Grouped changes into ${sourceSetChanges.size} affected source sets" }
            sourceSetChanges
        }

    /**
     * Analyzes all changes and groups them by source set with detailed change type information
     */
    suspend fun analyzeSourceSetChanges(
        properties: ApplicationProperties,
        allChanges: ChangesSet,
    ): Map<String, SourceSetChanges> =
        withContext(Dispatchers.Default) {
            logger.debug { "Analyzing ${allChanges.size} changes for source set grouping" }

            if (allChanges.isEmpty()) {
                logger.debug { "No changes to analyze" }
                return@withContext emptyMap()
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
                            logger.debug(e) { "Error checking if file $changedFile belongs to source set ${sourceItem.name}" }
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

            logger.info { "Analyzed changes for ${sourceSetChanges.size} affected source sets out of ${properties.sourceSet.size} total" }
            sourceSetChanges
        }

    /**
     * Determines if any source set has changes that require rebuilding
     */
    suspend fun requiresRebuild(sourceSetChanges: Map<String, SourceSetChanges>): Boolean = sourceSetChanges.values.any { it.hasChanges }

    /**
     * Gets summary statistics about source set changes
     */
    suspend fun getChangesSummary(sourceSetChanges: Map<String, SourceSetChanges>): SourceSetChangesSummary {
        val totalFiles = sourceSetChanges.values.sumOf { it.changedFiles.size }
        val affectedSourceSets = sourceSetChanges.size
        val totalNew =
            sourceSetChanges.values.sumOf { sourceSet ->
                sourceSet.changeTypes.values.count { it.first == ChangeType.NEW }
            }
        val totalModified =
            sourceSetChanges.values.sumOf { sourceSet ->
                sourceSet.changeTypes.values.count { it.first == ChangeType.MODIFIED }
            }
        val totalDeleted =
            sourceSetChanges.values.sumOf { sourceSet ->
                sourceSet.changeTypes.values.count { it.first == ChangeType.DELETED }
            }

        return SourceSetChangesSummary(
            affectedSourceSets = affectedSourceSets,
            totalChangedFiles = totalFiles,
            newFiles = totalNew,
            modifiedFiles = totalModified,
            deletedFiles = totalDeleted,
        )
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
) {
    val changeCount: Int get() = changedFiles.size
    val newFilesCount: Int get() = changeTypes.values.count { it.first == ChangeType.NEW }
    val modifiedFilesCount: Int get() = changeTypes.values.count { it.first == ChangeType.MODIFIED }
    val deletedFilesCount: Int get() = changeTypes.values.count { it.first == ChangeType.DELETED }
}

/**
 * Summary statistics for source set changes analysis
 */
data class SourceSetChangesSummary(
    val affectedSourceSets: Int,
    val totalChangedFiles: Int,
    val newFiles: Int,
    val modifiedFiles: Int,
    val deletedFiles: Int,
) {
    val hasChanges: Boolean get() = totalChangedFiles > 0
    val changePercentage: Double
        get() =
            if (totalChangedFiles > 0) {
                ((newFiles + modifiedFiles + deletedFiles).toDouble() / totalChangedFiles) * 100
            } else {
                0.0
            }
}
