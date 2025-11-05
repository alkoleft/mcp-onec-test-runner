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

package io.github.alkoleft.mcp.application.actions.test.yaxunit

import io.github.alkoleft.mcp.application.actions.common.ActionState
import io.github.alkoleft.mcp.application.actions.common.ActionStepResult
import io.github.alkoleft.mcp.application.actions.common.RunTestAction
import io.github.alkoleft.mcp.application.actions.common.RunTestResult
import io.github.alkoleft.mcp.application.actions.common.toActionStepResult
import io.github.alkoleft.mcp.application.actions.exceptions.TestExecuteException
import io.github.alkoleft.mcp.infrastructure.yaxunit.LogParser
import io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParser
import io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitRunner
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger { }

/**
 * Реализация RunTestAction для тестирования через YaXUnit.
 * Поддерживает запуск всех тестов, тестов модуля и конкретных тестов
 * Интегрирован со стратегиями обработки ошибок
 */
class YaXUnitTestAction(
    private val reportParser: ReportParser,
    private val runner: YaXUnitRunner,
) : RunTestAction {
    override fun run(request: TestExecutionRequest): RunTestResult {
        logger.info { "Запуск выполнения тестов YaXUnit с фильтром: $request" }
        val start = TimeSource.Monotonic.markNow()

        val state = TestActionState()

        try {
            val executionResult = runner.executeTests(request)
            state.setTestResult(executionResult)
            measureTimedValue { parseTestReport(executionResult) }
                .also { (report, duration) ->
                    state.setReport(report, duration)
                }
            executionResult.logPath?.also { state.setLogErrors(LogParser().extractErrors(it)) }

            return state.toResult(start.elapsedNow())
        } catch (e: Exception) {
            val duration = start.elapsedNow()
            logger.error(e) { "Выполнение тестов YaXUnit завершилось с ошибкой после ${duration.inWholeSeconds}с" }
            throw TestExecuteException("Выполнение тестов YaXUnit завершилось с ошибкой: ${e.message}", e)
        }
    }

    /**
     * Парсит отчет о тестировании
     */
    private fun parseTestReport(executionResult: YaXUnitExecutionResult): GenericTestReport? =
        if (executionResult.reportPath != null && Files.exists(executionResult.reportPath)) {
            try {
                logger.debug { "Парсинг отчета о тестах из: ${executionResult.reportPath}" }

                val inputStream = Files.newInputStream(executionResult.reportPath)
                inputStream.close()

                val report =
                    Files.newInputStream(executionResult.reportPath).use {
                        reportParser.parseReport(it)
                    }

                logger.info { "Отчет о тестах успешно проанализирован: ${report.summary.totalTests} тестов" }
                report
            } catch (e: Exception) {
                logger.warn(e) { "Не удалось проанализировать отчет о тестах из ${executionResult.reportPath}" }
                null
            }
        } else {
            logger.warn { "Отчет о тестах не найден по ожидаемому пути" }
            null
        }

    private class TestActionState : ActionState(logger) {
        lateinit var executionResult: YaXUnitExecutionResult
        var report: GenericTestReport? = null
        val errors: MutableList<String> = mutableListOf()

        fun setTestResult(result: YaXUnitExecutionResult) {
            executionResult = result
            if (!result.commandResult.success) {
                success = false
            }
            addStep(
                executionResult.commandResult.toActionStepResult(
                    "Выполнение тестов",
                    "Конфигурация запуска yaxunit:\n${executionResult.configPath};\nлог yaxunit: ${executionResult.logPath};\nполный отчет junit: ${executionResult.reportPath}",
                ),
            )
        }

        fun setLogErrors(value: List<String>) {
            if (value.isEmpty()) {
                return
            }
            success = false
            errors.addAll(value)
            logger.error { "Обнаружены ошибки в логе YAxUnit:\n" + value.joinToString("\n") }
        }

        fun setReport(
            value: GenericTestReport?,
            duration: Duration,
        ) {
            report = value
            if (value != null) {
                addStep(ActionStepResult("Отчет проанализирован: ${value.summary}", true, null, duration))
            } else {
                val error =
                    when {
                        executionResult.reportPath == null -> "YaXUnit не вернул путь к отчёту"
                        else -> "Не удалось прочитать отчёт YaXUnit"
                    }
                addStep(ActionStepResult("Не удалось проанализировать отчет о тестировании", false, error, duration))
                success = false
            }
        }

        fun toResult(duration: Duration) =
            RunTestResult(
                message = "Модульное тестирование yaxunit: " + if (success) "успешно. " else "неудачно. ",
                success = success,
                reportPath = executionResult.reportPath,
                logPath = executionResult.logPath,
                enterpriseLogPath = executionResult.commandResult.logFilePath?.toString(),
                report = report,
                duration = duration,
                steps = steps,
                errors = errors + steps.mapNotNull { it.error },
            )
    }
}
