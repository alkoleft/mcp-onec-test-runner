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

package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import io.github.alkoleft.mcp.application.actions.change.SourceSetChanges
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Интерфейс для сборки конфигурации и расширений
 */
interface BuildAction {
    fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult
}

interface ConvertAction {
    fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        destination: SourceSet,
    ): ConvertResult
}

/**
 * Интерфейс для анализа изменений в проекте
 */
interface ChangeAnalysisAction {
    fun run(properties: ApplicationProperties): ChangeAnalysisResult

    /**
     * Сохраняет состояние source set для инкрементальной сборки
     */
    fun saveSourceSetState(
        properties: ApplicationProperties,
        sourceSetChanges: SourceSetChanges,
    ): Boolean
}

/**
 * Интерфейс для запуска тестов
 */
interface RunTestAction {
    fun run(request: TestExecutionRequest): TestExecutionResult
}

interface ActionResult {
    val message: String
    val success: Boolean
    val errors: List<String>
    val duration: Duration
}

data class ActionStepResult(
    val message: String,
    val success: Boolean,
    val error: String?,
    val duration: Duration,
)

/**
 * Результат сборки
 */
data class BuildResult(
    override val message: String,
    override val success: Boolean,
    override val errors: List<String> = emptyList(),
    override val duration: Duration = Duration.ZERO,
    val sourceSet: Map<String, ShellCommandResult> = emptyMap(),
    val steps: List<ActionStepResult> = emptyList(),
) : ActionResult

/**
 * Результат сборки
 */
data class ConvertResult(
    val success: Boolean,
    val errors: List<String> = emptyList(),
    val duration: Duration = Duration.ZERO,
    val sourceSet: Map<String, ShellCommandResult> = emptyMap(),
    val steps: List<ActionStepResult> = emptyList(),
)

/**
 * Результат анализа изменений
 */
data class ChangeAnalysisResult(
    val hasChanges: Boolean,
    val changedFiles: Set<Path> = emptySet(),
    val changeTypes: ChangesSet = emptyMap(),
    val sourceSetChanges: Map<String, SourceSetChanges> = emptyMap(),
)
