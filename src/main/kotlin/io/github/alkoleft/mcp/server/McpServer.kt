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

import io.github.alkoleft.mcp.application.actions.common.LaunchRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunAllTestsRequest
import io.github.alkoleft.mcp.application.actions.test.yaxunit.RunModuleTestsRequest
import io.github.alkoleft.mcp.application.services.DesignerConfigCheckRequest
import io.github.alkoleft.mcp.application.services.DesignerModulesCheckRequest
import io.github.alkoleft.mcp.application.services.EdtCheckRequest
import io.github.alkoleft.mcp.application.services.LauncherService
import io.github.alkoleft.mcp.application.services.SyntaxCheckService
import io.github.alkoleft.mcp.server.dto.McpBuildResponse
import io.github.alkoleft.mcp.server.dto.McpLaunchResponse
import io.github.alkoleft.mcp.server.dto.McpSyntaxCheckResponse
import io.github.alkoleft.mcp.server.dto.McpTestResponse
import io.github.alkoleft.mcp.server.dto.toResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

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
    private val syntaxCheckService: SyntaxCheckService,
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

    /**
     * Выполняет синтаксис-проверку исходников через ЕДТ (validate)
     *
     * Выполняет синтаксис-проверку проекта через 1C:EDT CLI команду validate.
     * Проверяет все проекты типа CONFIGURATION из sourceSet.
     *
     * Процесс выполнения:
     * 1. Определение проектов для проверки из sourceSet
     * 2. Выполнение команды validate через EDT CLI
     * 3. Сохранение результатов в файл лога
     *
     * @return Результат выполнения проверки через ЕДТ
     * @throws Exception при возникновении ошибок во время выполнения проверки
     */
    @Tool(
        name = "check_syntax_edt",
        description = "Выполняет синтаксис-проверку исходников через ЕДТ (validate). Проверяет все проекты из sourceSet.",
    )
    fun checkSyntaxEdt(
        @ToolParam(
            description = "Имя проекта EDT для проверки. Если не указано, проверяются все проекты из sourceSet.",
            required = false,
        ) projectName: String?,
    ): McpSyntaxCheckResponse {
        logger.info { "Запуск синтаксис-проверки через ЕДТ" }

        return try {
            val result = measureTimedValue { syntaxCheckService.checkEdt(EdtCheckRequest(projectName)) }
            result.value.toResponse("через ЕДТ", result.duration)
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при выполнении проверки через ЕДТ" }
            McpSyntaxCheckResponse(
                success = false,
                message = "Ошибка при выполнении проверки через ЕДТ: ${e.message}",
                errors = listOf(e.message ?: "Неизвестная ошибка"),
            )
        }
    }

    /**
     * Выполняет синтаксис-проверку конфигурации через Конфигуратор (CheckConfig)
     *
     * Выполняет полную синтаксис-проверку конфигурации через команду CheckConfig конфигуратора 1С:Предприятие.
     * Проверяет всю конфигурацию без дополнительных параметров (пользователь может настроить параметры в будущем).
     *
     * Процесс выполнения:
     * 1. Подключение к информационной базе через конфигуратор
     * 2. Выполнение команды CheckConfig
     * 3. Сохранение результатов в файл лога
     *
     * @return Результат выполнения проверки CheckConfig
     * @throws Exception при возникновении ошибок во время выполнения проверки
     */
    @Tool(
        name = "check_syntax_designer_config",
        description = "Выполняет синтаксис-проверку конфигурации через Конфигуратор (CheckConfig). Проверяет всю конфигурацию.",
    )
    fun checkSyntaxDesignerConfig(
        @ToolParam(description = "Выполнять проверку логической целостности конфигурации", required = false) configLogIntegrity: Boolean?,
        @ToolParam(description = "Искать некорректные ссылки", required = false) incorrectReferences: Boolean?,
        @ToolParam(description = "Эмулировать режим тонкого клиента", required = false) thinClient: Boolean?,
        @ToolParam(description = "Эмулировать режим веб-клиента", required = false) webClient: Boolean?,
        @ToolParam(description = "Эмулировать режим мобильного клиента", required = false) mobileClient: Boolean?,
        @ToolParam(description = "Эмулировать режим сервера 1С:Предприятия", required = false) server: Boolean?,
        @ToolParam(description = "Эмулировать режим внешнего соединения в файловом режиме", required = false) externalConnection: Boolean?,
        @ToolParam(
            description = "Эмулировать режим внешнего соединения в клиент-серверном режиме",
            required = false,
        ) externalConnectionServer: Boolean?,
        @ToolParam(description = "Эмулировать режим клиента мобильного приложения", required = false) mobileAppClient: Boolean?,
        @ToolParam(description = "Эмулировать режим сервера мобильного приложения", required = false) mobileAppServer: Boolean?,
        @ToolParam(
            description = "Эмулировать режим управляемого приложения (толстый клиент) в файловом режиме",
            required = false,
        ) thickClientManagedApplication: Boolean?,
        @ToolParam(
            description = "Эмулировать режим управляемого приложения (толстый клиент) в клиент-серверном режиме",
            required = false,
        ) thickClientServerManagedApplication: Boolean?,
        @ToolParam(
            description = "Эмулировать режим обычного приложения (толстый клиент) в файловом режиме",
            required = false,
        ) thickClientOrdinaryApplication: Boolean?,
        @ToolParam(
            description = "Эмулировать режим обычного приложения (толстый клиент) в клиент-серверном режиме",
            required = false,
        ) thickClientServerOrdinaryApplication: Boolean?,
        @ToolParam(description = "Проверять корректность подписи мобильного клиента", required = false) mobileClientDigiSign: Boolean?,
        @ToolParam(description = "Проверять поставку модулей без исходных текстов", required = false) distributiveModules: Boolean?,
        @ToolParam(description = "Искать неиспользуемые процедуры и функции", required = false) unreferenceProcedures: Boolean?,
        @ToolParam(description = "Проверять существование назначенных обработчиков", required = false) handlersExistence: Boolean?,
        @ToolParam(description = "Искать пустые обработчики", required = false) emptyHandlers: Boolean?,
        @ToolParam(description = "Включить расширенную проверку модулей", required = false) extendedModulesCheck: Boolean?,
        @ToolParam(
            description = "Проверять использование синхронных вызовов (требует ExtendedModulesCheck)",
            required = false,
        ) checkUseSynchronousCalls: Boolean?,
        @ToolParam(
            description = "Проверять использование модальности (требует ExtendedModulesCheck)",
            required = false,
        ) checkUseModality: Boolean?,
        @ToolParam(
            description = "Искать неподдерживаемый функционал для мобильной платформы",
            required = false,
        ) unsupportedFunctional: Boolean?,
        @ToolParam(description = "Проверять только расширение с указанным именем", required = false) extension: String? = null,
        @ToolParam(description = "Проверять все расширения", required = false) allExtensions: Boolean?,
    ): McpSyntaxCheckResponse {
        logger.info { "Запуск синтаксис-проверки через Конфигуратор (CheckConfig)" }

        return try {
            val request =
                DesignerConfigCheckRequest(
                    configLogIntegrity = configLogIntegrity == true,
                    incorrectReferences = incorrectReferences == true,
                    thinClient = thinClient != false,
                    webClient = webClient == true,
                    mobileClient = mobileClient == true,
                    server = server != false,
                    externalConnection = externalConnection == true,
                    externalConnectionServer = externalConnectionServer == true,
                    mobileAppClient = mobileAppClient == true,
                    mobileAppServer = mobileAppServer == true,
                    thickClientManagedApplication = thickClientManagedApplication == true,
                    thickClientServerManagedApplication = thickClientServerManagedApplication == true,
                    thickClientOrdinaryApplication = thickClientOrdinaryApplication == true,
                    thickClientServerOrdinaryApplication = thickClientServerOrdinaryApplication == true,
                    mobileClientDigiSign = mobileClientDigiSign == true,
                    distributiveModules = distributiveModules == true,
                    unreferenceProcedures = unreferenceProcedures != false,
                    handlersExistence = handlersExistence != false,
                    emptyHandlers = emptyHandlers != false,
                    extendedModulesCheck = extendedModulesCheck != false,
                    checkUseSynchronousCalls = checkUseSynchronousCalls == true,
                    checkUseModality = checkUseModality == true,
                    unsupportedFunctional = unsupportedFunctional == true,
                    extension = extension,
                    allExtensions = allExtensions ?: (extension.isNullOrBlank()),
                )
            val result = measureTimedValue { syntaxCheckService.checkDesigner(request) }
            result.value.toResponse("CheckConfig", result.duration)
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при выполнении проверки CheckConfig" }
            McpSyntaxCheckResponse(
                success = false,
                message = "Ошибка при выполнении проверки CheckConfig: ${e.message}",
                errors = listOf(e.message ?: "Неизвестная ошибка"),
            )
        }
    }

    /**
     * Выполняет синтаксис-проверку модулей через Конфигуратор (CheckModules)
     *
     * Выполняет синтаксис-проверку модулей конфигурации через команду CheckModules конфигуратора 1С:Предприятие.
     * Пользователь может указать один или несколько режимов проверки и уточнить расширения для проверки.
     * По умолчанию проверяются все модули конфигурации.
     *
     * Процесс выполнения:
     * 1. Подключение к информационной базе через конфигуратор
     * 2. Формирование команды CheckModules с указанными режимами и параметрами расширений
     * 3. Сохранение результатов в файл лога
     *
     * @return Результат выполнения проверки CheckModules
     * @throws Exception при возникновении ошибок во время выполнения проверки
     */
    @Tool(
        name = "check_syntax_designer_modules",
        description = "Выполняет синтаксис-проверку модулей через Конфигуратор (CheckModules)",
    )
    fun checkSyntaxDesignerModules(
        @ToolParam(description = "Выполнять проверку в режиме тонкого клиента", required = false) thinClient: Boolean?,
        @ToolParam(description = "Выполнять проверку в режиме веб-клиента", required = false) webClient: Boolean?,
        @ToolParam(description = "Выполнять проверку в режиме сервера 1С:Предприятия", required = false) server: Boolean?,
        @ToolParam(description = "Выполнять проверку в режиме внешнего соединения", required = false) externalConnection: Boolean?,
        @ToolParam(
            description = "Выполнять проверку в режиме клиентского приложения (толстый клиент)",
            required = false,
        ) thickClientOrdinaryApplication: Boolean?,
        @ToolParam(description = "Выполнять проверку в режиме клиента мобильного приложения", required = false) mobileAppClient: Boolean?,
        @ToolParam(description = "Выполнять проверку в режиме сервера мобильного приложения", required = false) mobileAppServer: Boolean?,
        @ToolParam(description = "Выполнять проверку в режиме мобильного клиента", required = false) mobileClient: Boolean?,
        @ToolParam(description = "Включить расширенную проверку модулей", required = false) extendedModulesCheck: Boolean?,
        @ToolParam(description = "Проверять только расширение с указанным именем", required = false) extension: String? = null,
        @ToolParam(description = "Проверять все расширения", required = false) allExtensions: Boolean?,
    ): McpSyntaxCheckResponse {
        logger.info { "Запуск синтаксис-проверки через Конфигуратор (CheckModules)" }

        return try {
            val request =
                DesignerModulesCheckRequest(
                    thinClient = thinClient != false,
                    webClient = webClient == true,
                    server = server != false,
                    externalConnection = externalConnection == true,
                    thickClientOrdinaryApplication = thickClientOrdinaryApplication == true,
                    mobileAppClient = mobileAppClient == true,
                    mobileAppServer = mobileAppServer == true,
                    mobileClient = mobileClient == true,
                    extendedModulesCheck = extendedModulesCheck != false,
                    extension = extension,
                    allExtensions = allExtensions ?: (extension.isNullOrBlank()),
                )
            val result = measureTimedValue { syntaxCheckService.checkDesigner(request) }
            result.value.toResponse("CheckModules", result.duration)
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при выполнении проверки CheckModules" }
            val errorMessage = e.message ?: "Неизвестная ошибка"
            McpSyntaxCheckResponse(
                success = false,
                message = "Ошибка при выполнении проверки CheckModules: $errorMessage",
                errors = listOf(errorMessage),
            )
        }
    }
}
