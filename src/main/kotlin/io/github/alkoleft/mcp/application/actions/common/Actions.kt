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

package io.github.alkoleft.mcp.application.actions.common

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import io.github.alkoleft.mcp.application.actions.change.SourceSetChanges
import io.github.alkoleft.mcp.application.actions.test.yaxunit.GenericTestReport
import io.github.alkoleft.mcp.application.actions.test.yaxunit.TestExecutionRequest
import io.github.alkoleft.mcp.application.core.ShellCommandResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
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

/**
 * Результат сборки
 */
data class BuildResult(
    override val message: String,
    override val success: Boolean,
    override val errors: List<String> = emptyList(),
    override val duration: Duration = Duration.ZERO,
    override val steps: List<ActionStepResult> = emptyList(),
    val sourceSet: Map<String, ShellCommandResult> = emptyMap(),
) : ActionResult

interface ConvertAction {
    fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        destination: SourceSet,
    ): ConvertResult
}

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
 * Интерфейс для анализа изменений в проекте
 */
interface ChangeAnalysisAction {
    fun run(): ChangeAnalysisResult

    /**
     * Сохраняет состояние source set для инкрементальной сборки
     */
    fun saveSourceSetState(
        sourceSetChanges: SourceSetChanges,
        timeStamp: Long,
        success: Boolean,
    ): Boolean
}

/**
 * Результат анализа изменений
 */
data class ChangeAnalysisResult(
    val hasChanges: Boolean,
    val changedFiles: Set<Path> = emptySet(),
    val changeTypes: ChangesSet = emptyMap(),
    val sourceSetChanges: Map<String, SourceSetChanges> = emptyMap(),
    val steps: List<ActionStepResult>,
    val timestamp: Long,
)

/**
 * Интерфейс для запуска тестов
 */
interface RunTestAction {
    fun run(request: TestExecutionRequest): RunTestResult
}

/**
 * Test execution results
 */
data class RunTestResult(
    override val success: Boolean,
    override val duration: Duration,
    override val message: String,
    override val errors: List<String>,
    override val steps: List<ActionStepResult> = emptyList(),
    val report: GenericTestReport?,
    val reportPath: Path?,
    val enterpriseLogPath: String?,
    val logPath: String?,
) : ActionResult

interface ActionResult {
    val message: String
    val success: Boolean
    val errors: List<String>
    val duration: Duration
    val steps: List<ActionStepResult>
}

data class ActionStepResult(
    val message: String,
    val success: Boolean,
    val error: String? = null,
    val duration: Duration,
)
