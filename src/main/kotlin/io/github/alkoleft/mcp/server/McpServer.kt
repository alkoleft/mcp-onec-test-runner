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

package io.github.alkoleft.mcp.server

import io.github.alkoleft.mcp.application.actions.ActionStepResult
import io.github.alkoleft.mcp.application.services.LauncherService
import io.github.alkoleft.mcp.core.modules.GenericTestCase
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilities
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger { }

/**
 * MCP Server для YaXUnit Runner
 * Предоставляет инструменты для запуска тестов YaXUnit через Model Context Protocol
 */
@Service
class McpServer(
    private val launcherService: LauncherService,
    private val platformUtilities: PlatformUtilities,
) {
    /**
     * Запускает все тесты в проекте
     * @return Результат выполнения всех тестов
     */
    @Tool(
        name = "run_all_tests",
        description = "Запускает все тесты YaXUnit в проекте. Возвращает подробный отчет о выполнении тестов.",
    )
    fun runAllTests(): McpTestResponse {
        logger.info { "Запуск всех тестов YaXUnit" }

        try {
            val request = RunAllTestsRequest()
            val result = launcherService.run(request)
            return McpTestResponse(
                success = result.success,
                message = if (result.success) "Все тесты выполнены успешно" else "Ошибка при выполнении тестов",
                totalTests = result.report.summary.totalTests,
                passedTests = result.report.summary.passed,
                failedTests = result.report.summary.failed,
                executionTime = result.duration.inWholeMilliseconds,
                details =
                    mapOf(
                        "duration" to result.duration.toString(),
                        "successRate" to result.successRate,
                    ),
                testDetail = collectTestDetails(result.report),
            )
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при запуске всех тестов" }
            return McpTestResponse(
                success = false,
                message = "Ошибка при выполнении тестов: ${e.message}",
                totalTests = 0,
                passedTests = 0,
                failedTests = 0,
                executionTime = 0,
                details = mapOf("error" to e.message.toString()),
            )
        }
    }

    /**
     * Запускает тесты из указанного модуля
     * @param moduleName Имя модуля для тестирования
     * @return Результат выполнения тестов модуля
     */
    @Tool(
        name = "run_module_tests",
        description = "Запускает тесты YaXUnit из указанного модуля. Укажите имя модуля для тестирования.",
    )
    fun runModuleTests(
        @ToolParam(description = "Имя модуля для тестирования") moduleName: String,
    ): McpTestResponse {
        logger.info { "Запуск тестов модуля: $moduleName" }

        try {
            val request = RunModuleTestsRequest(moduleName)
            val result = launcherService.run(request)
            return McpTestResponse(
                success = result.success,
                message = if (result.success) "Тесты модуля '$moduleName' выполнены" else "Ошибка при выполнении тестов модуля",
                totalTests = result.report.summary.totalTests,
                passedTests = result.report.summary.passed,
                failedTests = result.report.summary.failed,
                executionTime = result.duration.inWholeMilliseconds,
                details =
                    mapOf(
                        "duration" to result.duration.toString(),
                        "successRate" to result.successRate,
                        "module" to moduleName,
                    ),
                testDetail = collectTestDetails(result.report),
            )
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при запуске тестов модуля: $moduleName" }
            return McpTestResponse(
                success = false,
                message = "Ошибка при выполнении тестов модуля '$moduleName': ${e.message}",
                totalTests = 0,
                passedTests = 0,
                failedTests = 0,
                executionTime = 0,
                details = mapOf("error" to e.message.toString(), "module" to moduleName),
            )
        }
    }

    fun collectTestDetails(report: GenericTestReport) = report.testSuites.flatMap { it.testCases }

    /**
     * Запускает тесты из списка указанных модулей
     * @param moduleNames Список имен модулей для тестирования
     * @return Результат выполнения тестов указанных модулей
     */
    @Tool(
        name = "run_list_tests",
        description = "Запускает тесты YaXUnit из списка указанных модулей. Передайте список имен модулей.",
    )
    fun runListTests(
        @ToolParam(description = "Список имен модулей для тестирования") moduleNames: List<String>,
    ): McpTestResponse {
        logger.info { "Запуск тестов модулей: $moduleNames" }

        try {
            val request = RunListTestsRequest(moduleNames)
            val result = launcherService.run(request)
            return McpTestResponse(
                success = result.success,
                message =
                    if (result.success) {
                        "Тесты модулей выполнены: ${moduleNames.joinToString(", ")}"
                    } else {
                        "Ошибка при выполнении тестов модулей"
                    },
                totalTests = result.report.summary.totalTests,
                passedTests = result.report.summary.passed,
                failedTests = result.report.summary.failed,
                executionTime = result.duration.inWholeMilliseconds,
                details =
                    mapOf(
                        "duration" to result.duration.toString(),
                        "successRate" to result.successRate,
                        "modules" to moduleNames.joinToString(", "),
                    ),
            )
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при запуске тестов модулей: $moduleNames" }
            return McpTestResponse(
                success = false,
                message = "Ошибка при выполнении тестов модулей '${moduleNames.joinToString(", ")}': ${e.message}",
                totalTests = 0,
                passedTests = 0,
                failedTests = 0,
                executionTime = 0,
                details = mapOf("error" to e.message.toString(), "modules" to moduleNames.joinToString(", ")),
            )
        }
    }

    /**
     * Выполняет сборку проекта
     * @return Результат сборки проекта
     */
    @Tool(
        name = "build_project",
        description = "Выполняет сборку проекта YaXUnit. Возвращает результат сборки.",
    )
    fun buildProject(): McpBuildResponse {
        logger.info { "Выполнение сборки проекта" }

        try {
            val start = TimeSource.Monotonic.markNow()
            val buildResult = launcherService.build()
            val duration = start.elapsedNow()

            return if (buildResult.success) {
                McpBuildResponse(
                    success = true,
                    message = buildResult.message,
                    buildTime = duration.inWholeMilliseconds,
                )
            } else {
                McpBuildResponse(
                    success = false,
                    message = if (buildResult.errors.isNotEmpty()) buildResult.errors.joinToString("; ") else "Ошибки сборки",
                    buildTime = duration.inWholeMilliseconds,
                    steps = buildResult.steps,
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при сборке проекта" }
            return McpBuildResponse(
                success = false,
                message = "Ошибка при сборке: ${e.message}",
            )
        }
    }
}

/**
 * Результат выполнения тестов
 */
data class McpTestResponse(
    val success: Boolean,
    val message: String,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val executionTime: Long,
    val details: Map<String, Any>,
    val testDetail: List<GenericTestCase> = emptyList(),
)

/**
 * Результат сборки проекта
 */
data class McpBuildResponse(
    val success: Boolean,
    val message: String,
    val buildTime: Long? = null,
    val steps: List<ActionStepResult>? = null,
)
