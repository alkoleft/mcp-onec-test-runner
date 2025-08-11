package io.github.alkoleft.mcp.server

import io.github.alkoleft.mcp.application.services.LauncherService
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.GenericTestCase
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger { }

/**
 * MCP Server для YaXUnit Runner
 * Предоставляет инструменты для запуска тестов YaXUnit через Model Context Protocol
 */
@Service
class YaXUnitMcpServer(
    private val launcherService: LauncherService,
    private val properties: ApplicationProperties,
) {
    /**
     * Запускает все тесты в проекте
     * @return Результат выполнения всех тестов
     */
    @Tool(
        name = "yaxunit_run_all_tests",
        description = "Запускает все тесты YaXUnit в проекте. Возвращает подробный отчет о выполнении тестов.",
    )
    fun runAllTests(): McpTestResponse {
        logger.info { "Запуск всех тестов YaXUnit" }

        return runBlocking {
            try {
                val request = RunAllTestsRequest(properties)
                val result = launcherService.run(request)
                McpTestResponse(
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
                McpTestResponse(
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
    }

    /**
     * Запускает тесты из указанного модуля
     * @param moduleName Имя модуля для тестирования
     * @return Результат выполнения тестов модуля
     */
    @Tool(
        name = "yaxunit_run_module_tests",
        description = "Запускает тесты YaXUnit из указанного модуля. Укажите имя модуля для тестирования.",
    )
    fun runModuleTests(
        @ToolParam(description = "Имя модуля для тестирования") moduleName: String,
    ): McpTestResponse {
        logger.info { "Запуск тестов модуля: $moduleName" }

        return runBlocking {
            try {
                val request = RunModuleTestsRequest(moduleName, properties)
                val result = launcherService.run(request)
                McpTestResponse(
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
                McpTestResponse(
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
    }

    fun collectTestDetails(report: GenericTestReport) = report.testSuites.flatMap { it.testCases }

    /**
     * Запускает тесты из списка указанных модулей
     * @param moduleNames Список имен модулей для тестирования
     * @return Результат выполнения тестов указанных модулей
     */
    @Tool(
        name = "yaxunit_run_list_tests",
        description = "Запускает тесты YaXUnit из списка указанных модулей. Передайте список имен модулей.",
    )
    fun runListTests(
        @ToolParam(description = "Список имен модулей для тестирования") moduleNames: List<String>,
    ): McpTestResponse {
        logger.info { "Запуск тестов модулей: $moduleNames" }

        return runBlocking {
            try {
                val request = RunListTestsRequest(moduleNames, properties)
                val result = launcherService.run(request)
                McpTestResponse(
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
                McpTestResponse(
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
    }

    /**
     * Выполняет сборку проекта
     * @return Результат сборки проекта
     */
    @Tool(
        name = "yaxunit_build_project",
        description = "Выполняет сборку проекта YaXUnit. Возвращает результат сборки.",
    )
    fun buildProject(): McpBuildResponse {
        logger.info { "Выполнение сборки проекта" }

        return runBlocking {
            try {
                val buildResult = launcherService.build()
                if (buildResult.success) {
                    McpBuildResponse(
                        success = true,
                        message = "Сборка выполнена успешно",
                        buildTime = buildResult.duration.inWholeMilliseconds,
                        details =
                            mapOf(
                                "configurationBuilt" to buildResult.configurationBuilt,
                                "errors" to buildResult.errors,
                            ),
                    )
                } else {
                    McpBuildResponse(
                        success = false,
                        message = if (buildResult.errors.isNotEmpty()) buildResult.errors.joinToString("; ") else "Ошибки сборки",
                        buildTime = buildResult.duration.inWholeMilliseconds,
                        details =
                            mapOf(
                                "configurationBuilt" to buildResult.configurationBuilt,
                                "errors" to buildResult.errors,
                            ),
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "Ошибка при сборке проекта" }
                McpBuildResponse(
                    success = false,
                    message = "Ошибка при сборке: ${e.message}",
                    buildTime = 0,
                    details = mapOf("error" to e.message.toString()),
                )
            }
        }
    }

    /**
     * Проверяет статус платформы 1С
     * @return Статус и информация о платформе 1С
     */
    @Tool(
        name = "yaxunit_check_platform",
        description = "Проверяет статус и доступность платформы 1С для выполнения тестов YaXUnit.",
    )
    fun checkPlatform(): PlatformStatusResult {
        logger.info { "Проверка статуса платформы 1С" }

        return try {
            // TODO: Implement platform status check
            PlatformStatusResult(
                success = true,
                message = "Платформа 1С доступна",
                version = "8.3.24.1482",
                path = "/usr/local/1cv8/8.3.24.1482/1cv8c",
                isAvailable = true,
                details = mapOf("platform" to "1C:Enterprise", "version" to "8.3.24.1482"),
            )
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при проверке платформы" }
            PlatformStatusResult(
                success = false,
                message = "Ошибка при проверке платформы: ${e.message}",
                version = null,
                path = null,
                isAvailable = false,
                details = mapOf("error" to e.message.toString()),
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
    val buildTime: Long,
    val details: Map<String, Any>,
)

/**
 * Результат статуса платформы
 */
data class PlatformStatusResult(
    val success: Boolean,
    val message: String,
    val version: String?,
    val path: String?,
    val isAvailable: Boolean,
    val details: Map<String, Any>,
)
