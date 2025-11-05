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

package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.ActionFactory
import io.github.alkoleft.mcp.application.actions.common.ActionStepResult
import io.github.alkoleft.mcp.application.actions.common.BuildResult
import io.github.alkoleft.mcp.application.actions.common.ConvertResult
import io.github.alkoleft.mcp.application.actions.common.RunTestResult
import io.github.alkoleft.mcp.application.actions.exceptions.AnalysisError
import io.github.alkoleft.mcp.application.actions.exceptions.TestExecutionError
import io.github.alkoleft.mcp.application.actions.test.yaxunit.TestExecutionRequest
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ProjectFormat
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import kotlin.time.Duration
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger { }

@Service
class LauncherService(
    private val actionFactory: ActionFactory,
    private val properties: ApplicationProperties,
) {
    private val edtSourceSet: SourceSet = createEdtSourceSet()
    private val designerSourceSet: SourceSet = createDesignerSourceSet()

    fun runTests(request: TestExecutionRequest): RunTestResult {
        val start = TimeSource.Monotonic.markNow()
        val buildResult = build()
        if (!buildResult.success) {
            val reason = if (buildResult.errors.isNotEmpty()) buildResult.errors.joinToString("; ") else "Сборка не удалась"
            throw TestExecutionError(reason)
        }
        return actionFactory.createRunTestAction().run(request).let {
            it.copy(steps = buildResult.steps + it.steps, duration = start.elapsedNow())
        }
    }

    fun launch(request: LaunchRequest) = actionFactory.createLaunchAction().run(request)

    fun build(): BuildResult {
        val changeAnalyzer = actionFactory.createChangeAnalysisAction()
        val changes = changeAnalyzer.run()

        val steps = mutableListOf<ActionStepResult>()
        steps.addAll(changes.steps)

        if (!changes.hasChanges) {
            return BuildResult(
                message = "Исходные файлы не изменены. Обновление базы пропущено",
                success = true,
                errors = emptyList(),
                duration = Duration.ZERO,
                sourceSet = emptyMap(),
                steps = steps,
            ).also { logger.info { it.message } }
        }
        val changedSourceSets = properties.sourceSet.subSourceSet { it.name in changes.sourceSetChanges.keys }

        if (changedSourceSets.isEmpty()) {
            throw AnalysisError("Не удалось распределить изменения по подпроектам.")
        }
        logger.info { "Обнаружены изменения: ${changedSourceSets.joinToString { it.name }}" }

        if (properties.format == ProjectFormat.EDT) {
            val convertResult = convertSources(changedSourceSets, designerSourceSet)
            steps.addAll(convertResult.steps)
            if (!convertResult.success) {
                return BuildResult(
                    message = "Ошибки конвертации исходников EDT: ${convertResult.errors.joinToString()}",
                    success = false,
                    errors = convertResult.errors,
                    duration = Duration.ZERO,
                    sourceSet = emptyMap(),
                    steps = steps,
                ).also { logger.error { it.message } }
            }
        }

        val result = updateIB(changedSourceSets)

        var success = true
        val errors = mutableListOf<String>()
        result.sourceSet.forEach { (name, result) ->
            success = success && result.success
            changeAnalyzer.saveSourceSetState(changes.sourceSetChanges[name]!!, changes.timestamp, result.success)
            if (!result.success) {
                result.error?.takeIf { it.isNotBlank() }?.let { errors.add(it) }
            }
        }

        return result.copy(steps = steps + result.steps)
    }

    private fun convertSources(
        changedSourceSets: SourceSet,
        destination: SourceSet,
    ): ConvertResult =
        actionFactory.convertAction().run(
            properties,
            edtSourceSet.subSourceSet { changedSourceSets.find { item -> item.name == it.name } != null },
            destination,
        )

    private fun updateIB(changedSourceSets: SourceSet): BuildResult {
        val builder = actionFactory.createBuildAction(properties.tools.builder)
        return builder.run(
            properties,
            designerSourceSet.subSourceSet { changedSourceSets.find { item -> item.name == it.name } != null },
        )
    }

    private fun createEdtSourceSet() =
        if (properties.format == ProjectFormat.EDT) {
            SourceSet(
                properties.basePath,
                properties.sourceSet,
            )
        } else {
            SourceSet.EMPTY
        }

    private fun createDesignerSourceSet() =
        if (properties.format == ProjectFormat.EDT) {
            SourceSet(
                properties.workPath,
                properties.sourceSet.map { it.copy(path = it.name) },
            )
        } else {
            SourceSet(
                properties.basePath,
                properties.sourceSet,
            )
        }
}
