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

import io.github.alkoleft.mcp.application.actions.common.ActionStepResult
import io.github.alkoleft.mcp.application.actions.common.LaunchRequest
import io.github.alkoleft.mcp.application.actions.common.RunTestResult
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
 *
 * Предоставляет инструменты для запуска тестов YaXUnit через Model Context Protocol (MCP).
 * Этот сервис является точкой входа для всех MCP-инструментов, которые могут быть вызваны
 * через AI-ассистентов, поддерживающих протокол MCP.
 *
 * Основные возможности:
 * - Запуск всех тестов проекта
 * - Запуск тестов отдельного модуля
 * - Сборка проекта
 * - Запуск приложений 1С:Предприятие
 *
 * @param launcherService Сервис для выполнения операций запуска тестов, сборки и запуска приложений
 */
@Service
class McpServer(
    private val launcherService: LauncherService,
) {
    /**
     * Запускает все тесты в проекте
     *
     * Выполняет запуск всех тестов YaXUnit, найденных в проекте. Перед запуском тестов
     * автоматически выполняется анализ изменений и инкрементальная сборка (если обнаружены изменения).
     *
     * Процесс выполнения:
     * 1. Создание запроса на запуск всех тестов
     * 2. Делегирование выполнения в LauncherService
     * 3. Преобразование результата в формат MCP-ответа
     *
     * @return Результат выполнения всех тестов, содержащий статистику, детали тестов и информацию об ошибках
     * @throws Exception при возникновении ошибок во время выполнения тестов
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
     *
     * Выполняет запуск тестов YaXUnit только из указанного модуля. Полезно для фокусного
     * тестирования конкретной функциональности без запуска всего набора тестов.
     *
     * Процесс выполнения:
     * 1. Создание запроса на запуск тестов модуля с указанным именем
     * 2. Делегирование выполнения в LauncherService
     * 3. Преобразование результата в формат MCP-ответа
     *
     * @param moduleName Имя модуля для тестирования. Должно соответствовать имени модуля в конфигурации проекта
     * @return Результат выполнения тестов модуля, содержащий статистику и детали тестов
     * @throws Exception при возникновении ошибок во время выполнения тестов или если модуль не найден
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
     *
     * Выполняет сборку проекта 1С:Предприятие. Система автоматически определяет, какие модули
     * были изменены, и выполняет инкрементальную сборку только измененных частей проекта.
     *
     * Процесс выполнения:
     * 1. Анализ изменений в исходных файлах
     * 2. Определение затронутых модулей
     * 3. Выполнение сборки (инкрементальной или полной)
     * 4. Измерение времени выполнения
     *
     * @return Результат сборки проекта, содержащий статус, сообщение, время сборки и шаги выполнения (при ошибках)
     * @throws Exception при возникновении ошибок во время сборки
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

    /**
     * Запускает приложение указанного типа
     *
     * Запускает одно из приложений платформы 1С:Предприятие на основе переданного псевдонима.
     * Поддерживаются различные псевдонимы для каждого типа приложения.
     *
     * Поддерживаемые типы приложений и их псевдонимы:
     * - DESIGNER: "DESIGNER", "designer", "1cv8", "конфигуратор"
     * - THIN_CLIENT: "THIN_CLIENT", "thin_client", "1cv8c", "тонкий клиент"
     * - THICK_CLIENT: "THICK_CLIENT", "thick_client", "толстый клиент"
     *
     * @param utilityType Псевдоним типа приложения для запуска. Может быть любым из поддерживаемых псевдонимов
     * @return Результат запуска приложения, содержащий статус и сообщение
     * @throws Exception при возникновении ошибок во время запуска или если псевдоним не распознан
     */
    @Tool(
        name = "launch_app",
        description = """Запускает приложение указанного типа. 
            |Укажите тип приложения: 
            |* DESIGNER (designer, 1cv8, конфигуратор),
            |* THIN_CLIENT (thin_client, 1cv8c, тонкий клиент),
            |* THICK_CLIENT (thick_client, толстый клиент).""",
    )
    fun launchUtility(
        @ToolParam(description = "Псевдоним типа приложения для запуска") utilityType: String,
    ): McpLaunchResponse {
        logger.info { "Запуск приложения с псевдонимом: $utilityType" }
        try {
            val result = launcherService.launch(LaunchRequest(utilityType))
            return McpLaunchResponse(
                success = result.success,
                message = result.message,
            )
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при запуске приложения: $utilityType" }
            return McpLaunchResponse(
                success = false,
                message = "Ошибка при запуске приложения: ${e.message}",
            )
        }
    }
}

/**
 * Преобразует результат выполнения тестов в формат MCP-ответа
 *
 * Расширяет функциональность [RunTestResult], преобразуя внутренний формат результата
 * в формат, понятный MCP-клиентам. Извлекает статистику тестов, детали выполнения
 * и информацию об ошибках из отчета.
 *
 * @receiver Результат выполнения тестов, полученный от LauncherService
 * @return MCP-ответ с результатами выполнения тестов
 */
fun RunTestResult.toResponse() =
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
 * Результат выполнения тестов в формате MCP
 *
 * Содержит полную информацию о результатах выполнения тестов YaXUnit, включая
 * статистику, детали тестов, пути к логам и информацию об ошибках.
 *
 * @param success Успешность выполнения тестов (true, если все тесты прошли успешно)
 * @param message Сообщение о результате выполнения
 * @param totalTests Общее количество выполненных тестов
 * @param passedTests Количество успешно пройденных тестов
 * @param failedTests Количество проваленных тестов
 * @param executionTime Время выполнения тестов в миллисекундах
 * @param testDetail Детальная информация о наборах тестов (test suites)
 * @param steps Список шагов выполнения (заполняется только при ошибках)
 * @param errors Список ошибок, возникших во время выполнения
 * @param enterpriseLogPath Путь к логу 1С:Предприятие
 * @param logFile Путь к файлу лога выполнения тестов
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
 * Результат сборки проекта в формате MCP
 *
 * Содержит информацию о результатах сборки проекта 1С:Предприятие, включая
 * статус, сообщение, время сборки и детали шагов выполнения (при ошибках).
 *
 * @param success Успешность сборки (true, если сборка завершилась без ошибок)
 * @param message Сообщение о результате сборки
 * @param buildTime Время выполнения сборки в миллисекундах
 * @param steps Список шагов выполнения сборки (заполняется только при ошибках для диагностики)
 */
data class McpBuildResponse(
    val success: Boolean,
    val message: String,
    val buildTime: Long? = null,
    val steps: List<ActionStepResult>? = null,
)

/**
 * Результат запуска приложения в формате MCP
 *
 * Содержит информацию о результатах запуска приложения 1С:Предприятие.
 *
 * @param success Успешность запуска (true, если приложение запущено успешно)
 * @param message Сообщение о результате запуска (может содержать PID процесса или описание ошибки)
 */
data class McpLaunchResponse(
    val success: Boolean,
    val message: String,
)
