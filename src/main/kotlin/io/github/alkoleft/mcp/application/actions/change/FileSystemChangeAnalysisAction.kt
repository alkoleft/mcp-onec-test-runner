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

import io.github.alkoleft.mcp.application.actions.ChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.ChangeAnalysisResult
import io.github.alkoleft.mcp.application.actions.exceptions.AnalyzeException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.storage.FileBuildStateManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

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
    override fun run(properties: ApplicationProperties): ChangeAnalysisResult {
        logger.info { "Анализ изменений по source set для проекта: ${properties.basePath}" }
        try {
            // Use FileBuildStateManager's Enhanced Hybrid Hash Detection
            val changes = buildStateManager.checkChanges(properties)

            if (changes.isEmpty()) {
                logger.info { "Изменения в проекте не обнаружены" }
                return ChangeAnalysisResult(
                    hasChanges = false,
                    changedFiles = emptySet(),
                    changeTypes = emptyMap(),
                    sourceSetChanges = emptyMap(),
                )
            }

            logger.info { "Найдено ${changes.size} измененных файлов, анализ по source set" }

            // Group changes by source set
            val sourceSetChanges = sourceSetAnalyzer.analyzeSourceSetChanges(properties, changes)

            logger.info { "Изменения сгруппированы в ${sourceSetChanges.size} затронутых source sets" }

            return ChangeAnalysisResult(
                hasChanges = true,
                changedFiles = changes.keys,
                changeTypes = changes,
                sourceSetChanges = sourceSetChanges,
            )
        } catch (e: Exception) {
            logger.error(e) { "Анализ изменений source set завершился с ошибкой" }
            throw AnalyzeException("Анализ изменений source set завершился с ошибкой: ${e.message}", e)
        }
    }

    override fun saveSourceSetState(
        properties: ApplicationProperties,
        sourceSetChanges: SourceSetChanges,
    ): Boolean {
        logger.debug { "Сохранение состояния source set: ${sourceSetChanges.sourceSetName} в проекте: ${properties.basePath}" }

        return try {
            if (sourceSetChanges.changedFiles.isNotEmpty()) {
                // Calculate and store hashes for changed files in this source set
                val hashUpdates = sourceSetChanges.changeTypes.entries.associate { it.key to it.value.second }

                buildStateManager.updateHashes(hashUpdates)
                logger.info { "Обновлено ${hashUpdates.size} хешей файлов для source set: ${sourceSetChanges.sourceSetName}" }
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "Не удалось сохранить состояние source set: ${sourceSetChanges.sourceSetName}" }
            false
        }
    }
}
