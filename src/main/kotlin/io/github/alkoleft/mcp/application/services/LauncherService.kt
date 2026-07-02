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

import io.github.alkoleft.mcp.application.actions.change.SourceSetChanges
import io.github.alkoleft.mcp.application.actions.common.ActionStepResult
import io.github.alkoleft.mcp.application.actions.common.BuildAction
import io.github.alkoleft.mcp.application.actions.common.BuildMode
import io.github.alkoleft.mcp.application.actions.common.BuildResult
import io.github.alkoleft.mcp.application.actions.common.ChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.common.ChangeAnalysisResult
import io.github.alkoleft.mcp.application.actions.common.ConvertAction
import io.github.alkoleft.mcp.application.actions.common.ConvertResult
import io.github.alkoleft.mcp.application.actions.common.LaunchAction
import io.github.alkoleft.mcp.application.actions.common.LaunchRequest
import io.github.alkoleft.mcp.application.actions.common.RunTestAction
import io.github.alkoleft.mcp.application.actions.common.RunTestResult
import io.github.alkoleft.mcp.application.actions.convert.EdtInteractiveConvertAction
import io.github.alkoleft.mcp.application.actions.exceptions.AnalysisError
import io.github.alkoleft.mcp.application.actions.exceptions.TestExecutionError
import io.github.alkoleft.mcp.application.actions.test.yaxunit.TestExecutionRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunAllTestsRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunListTestsRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunModuleTestsRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.YaXUnitTestAction
import io.github.alkoleft.mcp.application.core.ShellCommandResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ProjectFormat
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.storage.SourceSetContext
import io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParser
import io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitRunner
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import kotlin.collections.plus
import kotlin.time.Duration
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger { }
private const val SKIP_MAIN_CONFIGURATION_CONVERSION_MESSAGE =
    "Конвертация основной конфигурации EDT: пропущена по параметру skipMainConfigurationUpdate"

@Service
class LauncherService(
    private val buildAction: BuildAction,
    private val changeAnalysisAction: ChangeAnalysisAction,
    private val launchAction: LaunchAction,
    private val platformDsl: PlatformDsl,
    private val reportParser: ReportParser,
    private val yaxUnitRunner: YaXUnitRunner,
    private val properties: ApplicationProperties,
    private val sourceSetsService: SourceSetsService,
) {
    fun runTests(request: TestExecutionRequest): RunTestResult {
        val start = TimeSource.Monotonic.markNow()
        val buildResult = build(mode = request.buildMode())
        if (!buildResult.success) {
            val reason = if (buildResult.errors.isNotEmpty()) buildResult.errors.joinToString("; ") else "Сборка не удалась"
            throw TestExecutionError(reason)
        }
        val runTestAction: RunTestAction = YaXUnitTestAction(reportParser, yaxUnitRunner)
        return runTestAction.run(request).let {
            it.copy(steps = buildResult.steps + it.steps, duration = start.elapsedNow())
        }
    }

    fun launch(request: LaunchRequest) = launchAction.run(request)

    fun build(
        fullRebuild: Boolean = false,
        mode: BuildMode = BuildMode.FULL,
    ): BuildResult {
        if (fullRebuild) {
            logger.debug { "Сброс состояния source set" }
            sourceSetsService.getAllSourceSets().forEach { it.hashStorage.clear() }
        }

        val steps = mutableListOf<ActionStepResult>()

        if (properties.format == ProjectFormat.EDT) {
            val convertResult = convertSources(mode)
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
            steps.addAll(convertResult.steps)
        }
        return buildSourceSet(steps, mode)
    }

    private fun convertSources(mode: BuildMode): ConvertResult {
        val sourceSetContext = sourceSetsService.getEdtSourceSet()!!

        val changes = changeAnalysisAction.run(sourceSetContext)
        val steps = mutableListOf<ActionStepResult>()
        steps.addAll(changes.steps)
        if (!changes.hasChanges) {
            return ConvertResult(
                message = "Исходные файлы не изменены. Конвертация исходников EDT -> Designer пропущена",
                success = true,
                errors = emptyList(),
                duration = Duration.ZERO,
                steps = steps,
            ).also { logger.info { it.message } }
        }
        val changedSourceSets = sourceSetContext.sourceSet.subSourceSet { it.name in changes.sourceSetChanges.keys }
        val sourceSetsForConversion = changedSourceSets.forConversion(mode)

        if (changedSourceSets.isEmpty()) {
            throw AnalysisError("Не удалось распределить изменения по подпроектам.")
        }
        logger.info { "Обнаружены изменения (EDT): ${changedSourceSets.joinToString { it.name }}" }

        if (mode == BuildMode.SKIP_MAIN_CONFIGURATION && changedSourceSets.configuration != null) {
            logger.info { "Конвертация основной конфигурации EDT пропущена" }
            steps.add(
                ActionStepResult(
                    message = SKIP_MAIN_CONFIGURATION_CONVERSION_MESSAGE,
                    success = true,
                    duration = Duration.ZERO,
                ),
            )
        }

        if (sourceSetsForConversion.isEmpty()) {
            return ConvertResult(
                message = "Конвертация исходников EDT -> Designer пропущена",
                success = true,
                errors = emptyList(),
                duration = Duration.ZERO,
                steps = steps,
                sourceSet = emptyMap(),
            ).also { logger.info { it.message } }
        }

        logger.info { "К EDT-конвертации выбраны: ${sourceSetsForConversion.joinToString { it.name }}" }

        val result = convertSources(sourceSetsForConversion)
        saveSourceSetState(sourceSetContext, result.sourceSet, changes)
        return result.copy(steps = steps + result.steps)
    }

    private fun saveSourceSetState(
        sourceSetContext: SourceSetContext,
        sourceSetResults: Map<String, ShellCommandResult>,
        changes: ChangeAnalysisResult,
    ) {
        sourceSetResults.forEach { name, result ->
            changeAnalysisAction.saveSourceSetState(sourceSetContext, changes.sourceSetChanges[name]!!, changes.timestamp, result.success)
        }
    }

    private fun buildSourceSet(
        steps: MutableList<ActionStepResult>,
        mode: BuildMode,
    ): BuildResult {
        val sourceSetContext = sourceSetsService.getDesignerSourceSet()!!
        val changes = changeAnalysisAction.run(sourceSetContext)
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
        val changedSourceSets = sourceSetContext.sourceSet.subSourceSet { it.name in changes.sourceSetChanges.keys }

        if (changedSourceSets.isEmpty()) {
            throw AnalysisError("Не удалось распределить изменения по подпроектам.")
        }
        logger.info { "Обнаружены изменения (DESIGNER): ${changedSourceSets.joinToString { it.name }}" }

        val result = updateIB(changedSourceSets, changes.sourceSetChanges, mode)
        saveSourceSetState(sourceSetContext, result.sourceSet, changes)

        return result.copy(steps = steps + result.steps)
    }

    private fun convertSources(changedSourceSets: SourceSet): ConvertResult {
        val designerSourceSet = sourceSetsService.getDesignerSourceSet()?.sourceSet ?: SourceSet.EMPTY

        val convertAction: ConvertAction = EdtInteractiveConvertAction(platformDsl)
        return convertAction.run(
            properties,
            changedSourceSets,
            designerSourceSet,
        )
    }

    /**
     * Обновляет информационную базу с поддержкой частичной загрузки.
     *
     * Автоматически выбирает режим загрузки (частичная или полная) на основе
     * количества измененных файлов и настройки partialLoadThreshold.
     */
    private fun updateIB(
        changedSourceSets: SourceSet,
        sourceSetChanges: Map<String, SourceSetChanges>,
        mode: BuildMode,
    ): BuildResult =
        buildAction.runPartial(
            properties,
            changedSourceSets,
            sourceSetChanges,
            mode,
        )

    private fun TestExecutionRequest.buildMode(): BuildMode =
        when (this) {
            is RunAllTestsRequest -> buildMode
            is RunModuleTestsRequest -> buildMode
            is RunListTestsRequest -> buildMode
        }

    private fun SourceSet.forConversion(mode: BuildMode): SourceSet =
        when (mode) {
            BuildMode.FULL -> this
            BuildMode.SKIP_MAIN_CONFIGURATION -> subSourceSet { it.type == SourceSetType.EXTENSION }
        }
}
