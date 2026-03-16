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

import io.github.alkoleft.mcp.application.actions.common.ActionState
import io.github.alkoleft.mcp.application.actions.common.ActionStepResult
import io.github.alkoleft.mcp.application.actions.common.ChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.common.ChangeAnalysisResult
import io.github.alkoleft.mcp.application.actions.exceptions.AnalyzeException
import io.github.alkoleft.mcp.application.services.SourceSetsService
import io.github.alkoleft.mcp.infrastructure.changes.Scanner
import io.github.alkoleft.mcp.infrastructure.storage.FileBuildStateManager
import io.github.alkoleft.mcp.infrastructure.storage.SourceSetContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger { }

/**
 * Реализация ChangeAnalysisAction для анализа изменений в файловой системе
 * с поддержкой нескольких source sets
 */
@Component
class FileSystemChangeAnalysisAction(
    private val sourceSetsService: SourceSetsService,
    private val sourceSetAnalyzer: SourceSetChangeAnalyzer,
    private val scanner: Scanner,
) : ChangeAnalysisAction {
    override fun run(sourceSetContext: SourceSetContext): ChangeAnalysisResult {
        val state = ChangeAnalysisActionState()

        try {
            logger.debug { "Анализ изменений для source set: ${sourceSetContext.name}" }

            // Create a temporary FileBuildStateManager for this source set
            val buildStateManager = FileBuildStateManager(sourceSetContext, scanner)
            val timedChanges = measureTimedValue { runBlocking { buildStateManager.checkChanges() } }

            state.addStep(
                ActionStepResult(
                    message =
                        buildString {
                            append("Анализ source set '${sourceSetContext.name}': ")
                            append(
                                if (timedChanges.value.isEmpty()) {
                                    "нет изменений"
                                } else {
                                    "найдено ${timedChanges.value.size} измененных файлов"
                                },
                            )
                        },
                    success = true,
                    duration = timedChanges.duration,
                ),
            )

            state.updateChanges(timedChanges.value)

            if (timedChanges.value.isEmpty()) {
                return state.toResult()
            }

            // Group changes by subproject for this source set
            val sourceSetChanges =
                sourceSetAnalyzer
                    .analyzeSourceSetChanges(sourceSetContext, timedChanges.value)
                    .also {
                        logger.info { "Изменения сгруппированы в ${it.size} подпроектов для source set '${sourceSetContext.name}'" }
                    }
            return state.toResult(sourceSetChanges)
        } catch (e: Exception) {
            logger.error(e) { "Анализ изменений завершился с ошибкой" }
            throw AnalyzeException("Анализ изменений завершился с ошибкой: ${e.message}", e)
        }
    }

    override fun saveSourceSetState(
        sourceSetContext: SourceSetContext,
        sourceSetChanges: SourceSetChanges,
        timeStamp: Long,
        success: Boolean,
    ): Boolean {
        logger.debug { "Сохранение состояния подпроекта: ${sourceSetChanges.sourceSetName}" }

        return try {
            val buildStateManager = FileBuildStateManager(sourceSetContext, scanner)

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
                buildStateManager.storeTimestamp(timeStamp)
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

        fun updateChanges(value: ChangesSet) {
            changes = value
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
