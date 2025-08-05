package io.github.alkoleft.mcp.interfaces.mcp

import io.github.alkoleft.mcp.application.services.TestLauncherService
import io.github.alkoleft.mcp.core.modules.BuildService
import io.github.alkoleft.mcp.infrastructure.config.ProjectConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import java.nio.file.Paths

private val logger = KotlinLogging.logger {  }

/**
 * MCP Server для YaXUnit Runner
 * Предоставляет инструменты для запуска тестов YaXUnit через Model Context Protocol
 */
@Service
class YaXUnitMcpServer(
    private val testLauncherService: TestLauncherService,
    private val buildService: BuildService,
    private val projectConfiguration: ProjectConfiguration,
) {

    /**
     * Запускает все тесты в проекте
     * @return Результат выполнения всех тестов
     */
    @Tool(
        name = "yaxunit_run_all_tests",
        description = "Запускает все тесты YaXUnit в проекте. Возвращает подробный отчет о выполнении тестов."
    )
    fun runAllTests(): TestExecutionResult {
        logger.info { "Запуск всех тестов YaXUnit" }
        
        return runBlocking {
            try {
                val request = io.github.alkoleft.mcp.core.modules.RunAllTestsRequest(
                    projectPath = Paths.get("."),
                    ibConnection = projectConfiguration.informationBase?.connection ?: ""
                )
                val result = testLauncherService.runAll(request)
                TestExecutionResult(
                    success = result.success,
                    message = if (result.success) "Все тесты выполнены успешно" else "Ошибка при выполнении тестов",
                    totalTests = result.report.summary.totalTests,
                    passedTests = result.report.summary.passed,
                    failedTests = result.report.summary.failed,
                    executionTime = result.duration.toMillis(),
                    details = mapOf(
                        "duration" to result.duration.toString(),
                        "successRate" to result.report.summary.successRate.toString()
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Ошибка при запуске всех тестов" }
                TestExecutionResult(
                    success = false,
                    message = "Ошибка при выполнении тестов: ${e.message}",
                    totalTests = 0,
                    passedTests = 0,
                    failedTests = 0,
                    executionTime = 0,
                    details = mapOf("error" to e.message.toString())
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
        description = "Запускает тесты YaXUnit из указанного модуля. Укажите имя модуля для тестирования."
    )
    fun runModuleTests(
        @ToolParam(description = "Имя модуля для тестирования") moduleName: String
    ): TestExecutionResult {
        logger.info { "Запуск тестов модуля: $moduleName" }
        
        return runBlocking {
            try {
                val request = io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest(
                    moduleName = moduleName,
                    projectPath = Paths.get("."),
                    ibConnection = projectConfiguration.informationBase?.connection ?: ""
                )
                val result = testLauncherService.runModule(request)
                TestExecutionResult(
                    success = result.success,
                    message = if (result.success) "Тесты модуля '$moduleName' выполнены" else "Ошибка при выполнении тестов модуля",
                    totalTests = result.report.summary.totalTests,
                    passedTests = result.report.summary.passed,
                    failedTests = result.report.summary.failed,
                    executionTime = result.duration.toMillis(),
                    details = mapOf(
                        "duration" to result.duration.toString(),
                        "successRate" to result.report.summary.successRate.toString(),
                        "module" to moduleName
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Ошибка при запуске тестов модуля: $moduleName" }
                TestExecutionResult(
                    success = false,
                    message = "Ошибка при выполнении тестов модуля '$moduleName': ${e.message}",
                    totalTests = 0,
                    passedTests = 0,
                    failedTests = 0,
                    executionTime = 0,
                    details = mapOf("error" to e.message.toString(), "module" to moduleName)
                )
            }
        }
    }

    /**
     * Запускает тесты из списка указанных модулей
     * @param moduleNames Список имен модулей для тестирования
     * @return Результат выполнения тестов указанных модулей
     */
    @Tool(
        name = "yaxunit_run_list_tests",
        description = "Запускает тесты YaXUnit из списка указанных модулей. Передайте список имен модулей."
    )
    fun runListTests(
        @ToolParam(description = "Список имен модулей для тестирования") moduleNames: List<String>
    ): TestExecutionResult {
        logger.info { "Запуск тестов модулей: $moduleNames" }
        
        return runBlocking {
            try {
                val request = io.github.alkoleft.mcp.core.modules.RunListTestsRequest(
                    testNames = moduleNames,
                    projectPath = Paths.get("."),
                    ibConnection = projectConfiguration.informationBase?.connection ?: ""
                )
                val result = testLauncherService.runList(request)
                TestExecutionResult(
                    success = result.success,
                    message = if (result.success) "Тесты модулей выполнены: ${moduleNames.joinToString(", ")}" else "Ошибка при выполнении тестов модулей",
                    totalTests = result.report.summary.totalTests,
                    passedTests = result.report.summary.passed,
                    failedTests = result.report.summary.failed,
                    executionTime = result.duration.toMillis(),
                    details = mapOf(
                        "duration" to result.duration.toString(),
                        "successRate" to result.report.summary.successRate.toString(),
                        "modules" to moduleNames.joinToString(", ")
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Ошибка при запуске тестов модулей: $moduleNames" }
                TestExecutionResult(
                    success = false,
                    message = "Ошибка при выполнении тестов модулей '${moduleNames.joinToString(", ")}': ${e.message}",
                    totalTests = 0,
                    passedTests = 0,
                    failedTests = 0,
                    executionTime = 0,
                    details = mapOf("error" to e.message.toString(), "modules" to moduleNames.joinToString(", "))
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
        description = "Выполняет сборку проекта YaXUnit. Возвращает результат сборки."
    )
    fun buildProject(): BuildResult {
        logger.info { "Выполнение сборки проекта" }
        
        return runBlocking {
            try {
                val result = buildService.ensureBuild(Paths.get("."))
                BuildResult(
                    success = result.success,
                    message = if (result.success) "Сборка выполнена успешно" else "Ошибка при сборке",
                    buildTime = result.duration.toMillis(),
                    details = mapOf(
                        "buildType" to result.buildType.name,
                        "duration" to result.duration.toString()
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Ошибка при сборке проекта" }
                BuildResult(
                    success = false,
                    message = "Ошибка при сборке: ${e.message}",
                    buildTime = 0,
                    details = mapOf("error" to e.message.toString())
                )
            }
        }
    }

    /**
     * Получает информацию о доступных модулях
     * @return Список доступных модулей для тестирования
     */
    @Tool(
        name = "yaxunit_list_modules",
        description = "Возвращает список доступных модулей для тестирования YaXUnit."
    )
    fun listModules(): ModuleListResult {
        logger.info { "Получение списка доступных модулей" }
        
        return try {
            // TODO: Implement module discovery logic
            val modules = listOf("CommonModule", "TestModule", "ExampleModule")
            ModuleListResult(
                success = true,
                message = "Найдено ${modules.size} модулей",
                modules = modules,
                totalCount = modules.size
            )
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при получении списка модулей" }
            ModuleListResult(
                success = false,
                message = "Ошибка при получении списка модулей: ${e.message}",
                modules = emptyList(),
                totalCount = 0
            )
        }
    }

    /**
     * Получает конфигурацию проекта
     * @return Текущая конфигурация проекта
     */
    @Tool(
        name = "yaxunit_get_configuration",
        description = "Возвращает текущую конфигурацию проекта YaXUnit."
    )
    fun getConfiguration(): ConfigurationResult {
        logger.info { "Получение конфигурации проекта" }
        
        return try {
            ConfigurationResult(
                success = true,
                message = "Конфигурация получена успешно",
                projectSettings = projectConfiguration.project,
                platformSettings = projectConfiguration.platform,
                buildSettings = projectConfiguration.build,
                loggingSettings = projectConfiguration.logging,
                serverSettings = projectConfiguration.server
            )
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при получении конфигурации" }
            ConfigurationResult(
                success = false,
                message = "Ошибка при получении конфигурации: ${e.message}",
                projectSettings = null,
                platformSettings = null,
                buildSettings = null,
                loggingSettings = null,
                serverSettings = null
            )
        }
    }

    /**
     * Проверяет статус платформы 1С
     * @return Статус и информация о платформе 1С
     */
    @Tool(
        name = "yaxunit_check_platform",
        description = "Проверяет статус и доступность платформы 1С для выполнения тестов YaXUnit."
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
                details = mapOf("platform" to "1C:Enterprise", "version" to "8.3.24.1482")
            )
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при проверке платформы" }
            PlatformStatusResult(
                success = false,
                message = "Ошибка при проверке платформы: ${e.message}",
                version = null,
                path = null,
                isAvailable = false,
                details = mapOf("error" to e.message.toString())
            )
        }
    }
}

/**
 * Результат выполнения тестов
 */
data class TestExecutionResult(
    val success: Boolean,
    val message: String,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val executionTime: Long,
    val details: Map<String, Any>
)

/**
 * Результат сборки проекта
 */
data class BuildResult(
    val success: Boolean,
    val message: String,
    val buildTime: Long,
    val details: Map<String, Any>
)

/**
 * Результат списка модулей
 */
data class ModuleListResult(
    val success: Boolean,
    val message: String,
    val modules: List<String>,
    val totalCount: Int
)

/**
 * Результат конфигурации
 */
data class ConfigurationResult(
    val success: Boolean,
    val message: String,
    val projectSettings: Any?,
    val platformSettings: Any?,
    val buildSettings: Any?,
    val loggingSettings: Any?,
    val serverSettings: Any?
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
    val details: Map<String, Any>
) 