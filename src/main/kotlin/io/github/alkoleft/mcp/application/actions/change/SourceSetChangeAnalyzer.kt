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
class SourceSetChangeAnalyzer(
    private val properties: ApplicationProperties,
) {
    /**
     * Analyzes all changes and groups them by source set with detailed change type information
     */
    fun analyzeSourceSetChanges(allChanges: ChangesSet): Map<String, SourceSetChanges> {
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
)
