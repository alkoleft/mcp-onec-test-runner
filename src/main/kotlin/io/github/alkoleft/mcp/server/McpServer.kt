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
import io.github.alkoleft.mcp.application.actions.TestExecutionResult
import io.github.alkoleft.mcp.application.actions.test.yaxunit.GenericTestSuite
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunAllTestsRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunModuleTestsRequest
import io.github.alkoleft.mcp.application.services.LauncherService
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
            val result = launcherService.runTests(request)
            return result.toResponse()
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при запуске всех тестов" }
            return McpTestResponse(
                success = false,
                message = "Ошибка при выполнении тестов",
                errors = listOf(e.message.toString()),
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
            val result = launcherService.runTests(request)
            return result.toResponse()
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при запуске тестов модуля: $moduleName" }
            return McpTestResponse(
                success = false,
                message = "Ошибка при выполнении тестов модуля '$moduleName'",
                errors = listOf(e.message.toString()),
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

fun TestExecutionResult.toResponse() =
    McpTestResponse(
        success = success,
        message = message,
        logFile = logPath,
        enterpriseLogPath = enterpriseLogPath,
        totalTests = report?.summary?.totalTests,
        passedTests = report?.summary?.passed,
        failedTests = report?.summary?.failed,
        executionTime = duration.inWholeMilliseconds,
        testDetail = report?.testSuites,
        steps = if (success) null else steps,
        errors = errors,
    )

/**
 * Результат выполнения тестов
 */
data class McpTestResponse(
    val success: Boolean,
    val message: String,
    val totalTests: Int? = null,
    val passedTests: Int? = null,
    val failedTests: Int? = null,
    val executionTime: Long? = null,
    val testDetail: List<GenericTestSuite>? = null,
    val steps: List<ActionStepResult>? = null,
    val errors: List<String>,
    val enterpriseLogPath: String? = null,
    val logFile: String? = null,
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
