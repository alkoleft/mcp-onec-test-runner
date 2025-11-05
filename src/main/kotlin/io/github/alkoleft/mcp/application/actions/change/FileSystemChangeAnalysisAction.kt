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

import io.github.alkoleft.mcp.application.actions.ActionStepResult
import io.github.alkoleft.mcp.application.actions.ChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.ChangeAnalysisResult
import io.github.alkoleft.mcp.application.actions.common.ActionState
import io.github.alkoleft.mcp.application.actions.exceptions.AnalyzeException
import io.github.alkoleft.mcp.infrastructure.storage.FileBuildStateManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

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
    override fun run(): ChangeAnalysisResult {
        logger.info { "Анализ изменений по проекта" }

        val state = ChangeAnalysisActionState()
        try {
            state.setChanges(measureTimedValue { buildStateManager.checkChanges() })

            if (state.changes.isEmpty()) {
                return state.toResult()
            }

            // Group changes by подпроекта
            val sourceSetChanges =
                sourceSetAnalyzer
                    .analyzeSourceSetChanges(state.changes)
                    .also { logger.info { "Изменения сгруппированы в ${it.size} затронутых подпроекта" } }

            return state.toResult(sourceSetChanges)
        } catch (e: Exception) {
            logger.error(e) { "Анализ изменений завершился с ошибкой" }
            throw AnalyzeException("Анализ изменений завершился с ошибкой: ${e.message}", e)
        }
    }

    override fun saveSourceSetState(
        sourceSetChanges: SourceSetChanges,
        timeStamp: Long,
        success: Boolean,
    ): Boolean {
        logger.debug { "Сохранение состояния подпроекта: ${sourceSetChanges.sourceSetName}" }

        return try {
            if (sourceSetChanges.changedFiles.isNotEmpty()) {
                // Calculate and store hashes for changed files in this source set
                val hashUpdates =
                    if (success) {
                        sourceSetChanges.changeTypes.entries.associate { it.key to it.value.second }
                    } else {
                        sourceSetChanges.changeTypes.entries.associate { it.key to "" }
                    }

                buildStateManager.updateHashes(hashUpdates)

                logger.debug {
                    "${if (success) "Обновлено" else "Очищено"} ${hashUpdates.size} хешей файлов для подпроекта: ${sourceSetChanges.sourceSetName}"
                }
            }
            if (success) {
                buildStateManager.storeTimestamp(sourceSetChanges.sourceSetName, timeStamp)
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "Не удалось сохранить состояние подпроекта: ${sourceSetChanges.sourceSetName}" }
            false
        }
    }

    private class ChangeAnalysisActionState : ActionState(logger) {
        lateinit var changes: ChangesSet
        val timestamp = System.currentTimeMillis()

        fun setChanges(value: TimedValue<ChangesSet>) {
            changes = value.value
            addStep(
                ActionStepResult(
                    message =
                        "Анализ изменений: " +
                            if (value.value.isEmpty()) "нет изменений" else "найдено ${value.value.size} измененных файлов",
                    success = true,
                    duration = value.duration,
                ),
            )
        }

        fun toResult(sourceSetChanges: Map<String, SourceSetChanges> = emptyMap()): ChangeAnalysisResult =
            ChangeAnalysisResult(
                hasChanges = !changes.isEmpty(),
                changedFiles = changes.keys,
                changeTypes = changes,
                sourceSetChanges = sourceSetChanges,
                steps = steps.toList(),
                timestamp = timestamp,
            )
    }
}
