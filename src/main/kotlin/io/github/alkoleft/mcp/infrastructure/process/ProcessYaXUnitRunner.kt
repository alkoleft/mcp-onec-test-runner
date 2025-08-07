package io.github.alkoleft.mcp.infrastructure.process

import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.YaXUnitRunner
import io.github.alkoleft.mcp.core.modules.YaXUnitExecutionResult
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.ConfiguratorContext
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdPlan
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdPlanDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.nio.file.Files
import kotlin.time.Duration as KotlinDuration

private val logger = KotlinLogging.logger { }

/**
 * Реализация YaXUnitRunner для запуска тестов через 1С:Предприятие
 */
class ProcessYaXUnitRunner(
    private val utilLocator: CrossPlatformUtilLocator,
    private val configWriter: JsonYaXUnitConfigWriter
) : YaXUnitRunner {

    override suspend fun executeTests(
        utilityLocation: UtilityLocation,
        configPath: Path,
        request: TestExecutionRequest
    ): YaXUnitExecutionResult = withContext(Dispatchers.IO) {
        val startTime = Instant.now()
        logger.info { "Starting YaXUnit test execution for ${request.javaClass.simpleName}" }
        
        try {
            // Создаем временную конфигурацию для запуска тестов
            logger.debug { "Creating temporary configuration at: $configPath" }
            val tempConfigPath = configWriter.createTempConfig(request)
            logger.debug { "Configuration created successfully" }
            
            // Формируем команду запуска 1С:Предприятие с параметром RunUnitTests
            logger.debug { "Building enterprise command arguments" }
            val commandArgs = buildEnterpriseCommandArgs(
                utilityLocation,
                request,
                tempConfigPath
            )
            logger.info { "Command: ${commandArgs.joinToString(" ")}" }
            
            // Выполняем команду
            logger.info { "Executing test process..." }
            val processResult = executeProcess(commandArgs)
            logger.info { "Process completed with exit code: ${processResult.exitCode}" }
            
            val duration = Duration.between(startTime, Instant.now())
            logger.info { "Test execution completed in ${duration.toSeconds()}s" }
            
            // Определяем путь к отчету
            val reportPath = determineReportPath(request, tempConfigPath)
            if (reportPath != null && Files.exists(reportPath)) {
                logger.info { "Test report found at: $reportPath" }
            } else {
                logger.warn { "Test report not found at expected location" }
            }
            
            YaXUnitExecutionResult(
                success = processResult.success,
                reportPath = if (processResult.success) reportPath else null,
                exitCode = processResult.exitCode,
                standardOutput = processResult.output,
                errorOutput = processResult.error ?: "",
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now())
            logger.error(e) { "YaXUnit test execution failed after ${duration.toSeconds()}s" }
            YaXUnitExecutionResult(
                success = false,
                reportPath = null,
                exitCode = -1,
                standardOutput = "",
                errorOutput = e.message ?: "Unknown error",
                duration = duration
            )
        }
    }
    
    /**
     * Строит аргументы команды для запуска 1С:Предприятие
     */
    private fun buildEnterpriseCommandArgs(
        utilityLocation: UtilityLocation,
        request: TestExecutionRequest,
        configPath: Path
    ): List<String> {
        val args = mutableListOf<String>()
        
        // Путь к исполняемому файлу 1С:Предприятие
        args.add(utilityLocation.executablePath.toString())
        args.add("ENTERPRISE")
        
        // Параметры подключения к информационной базе
        logger.debug { "Building connection arguments for: ${request.ibConnection}" }
        args.addAll(buildConnectionArgs(request))
        
        // Параметры авторизации если указаны
        args.addAll(buildAuthArgs(request))
        
        // Параметр запуска тестов
        args.add("/C")
        args.add("RunUnitTests=${configPath.toAbsolutePath()}")
        
        logger.debug { "Built command arguments: ${args.joinToString(" ")}" }
        return args
    }
    
    /**
     * Строит аргументы подключения к информационной базе
     */
    private fun buildConnectionArgs(request: TestExecutionRequest): List<String> {
        val args = mutableListOf<String>()
        
        // Парсим строку подключения
        val connectionString = request.ibConnection
        logger.debug { "Parsing connection string: $connectionString" }
        
        // Если это файловая база
        if (connectionString.startsWith("File=")) {
            val dbPath = connectionString.substringAfter("File=").substringBefore(";")
            logger.debug { "File database detected: $dbPath" }
            args.add("/IBName")
            args.add(dbPath)
        } else {
            // Для серверной базы
            logger.debug { "Server database detected" }
            args.add("/IBConnectionString")
            args.add(connectionString)
        }
        
        return args
    }
    
    /**
     * Строит аргументы авторизации
     */
    private fun buildAuthArgs(request: TestExecutionRequest): List<String> {
        val args = mutableListOf<String>()
        
        // Парсим строку подключения для извлечения параметров авторизации
        val connectionString = request.ibConnection
        
        // Ищем параметры авторизации в строке подключения
        val authParams = connectionString.split(";")
            .filter { it.contains("=") }
            .associate { 
                val (key, value) = it.split("=", limit = 2)
                key.trim() to value.trim()
            }
        
        // Добавляем параметры авторизации если они есть
        authParams["N"]?.let { username ->
            logger.debug { "Adding username parameter: $username" }
            args.add("/N")
            args.add(username)
        }
        
        authParams["P"]?.let { password ->
            logger.debug { "Adding password parameter" }
            args.add("/P")
            args.add(password)
        }
        
        return args
    }
    
    /**
     * Выполняет процесс запуска тестов
     */
    private suspend fun executeProcess(commandArgs: List<String>): PlatformUtilityResult {
        val processStartTime = Instant.now()
        logger.debug { "Starting process with command: ${commandArgs.joinToString(" ")}" }
        
        val processBuilder = ProcessBuilder(commandArgs)
        processBuilder.redirectErrorStream(true)
        
        // Устанавливаем рабочую директорию
        val workingDir = commandArgs.first().let { Path.of(it) }.parent
        if (workingDir != null && Files.exists(workingDir)) {
            processBuilder.directory(workingDir.toFile())
            logger.debug { "Set working directory to: $workingDir" }
        }
        
        val process = processBuilder.start()
        logger.debug { "Process started with PID: ${process.pid()}" }
        
        // Читаем вывод процесса
        val output = process.inputStream.bufferedReader().readText()
        logger.debug { "Process output length: ${output.length} characters" }
        
        val exitCode = process.waitFor()
        val processDuration = Duration.between(processStartTime, Instant.now())
        logger.info { "Process completed with exit code $exitCode in ${processDuration.toSeconds()}s" }
        
        return PlatformUtilityResult(
            success = exitCode == 0,
            output = output,
            error = if (exitCode != 0) "Process exited with code $exitCode" else null,
            exitCode = exitCode,
            duration = KotlinDuration.parse(processDuration.toString())
        )
    }
    
    /**
     * Определяет путь к отчету о тестировании
     */
    private fun determineReportPath(request: TestExecutionRequest, configPath: Path): Path? {
        // Пытаемся найти отчет в нескольких возможных местах
        val possiblePaths = listOf(
            request.testsPath.resolve("reports").resolve("report.xml"),
            request.testsPath.resolve("reports").resolve("junit.xml"),
            request.testsPath.resolve("report.xml"),
            request.testsPath.resolve("junit.xml"),
            configPath.parent.resolve("report.xml"),
            configPath.parent.resolve("junit.xml")
        )
        
        logger.debug { "Searching for test report in possible paths: ${possiblePaths.joinToString(", ")}" }
        
        for (path in possiblePaths) {
            if (Files.exists(path)) {
                logger.info { "Found test report at: $path" }
                return path
            }
        }
        
        logger.warn { "Test report not found in any expected location" }
        return null
    }
    
    /**
     * Запускает тесты через ibcmd (альтернативный способ)
     * TODO: Реализовать в будущих версиях
     */
    suspend fun executeTestsViaIbcmd(
        request: TestExecutionRequest,
        properties: ApplicationProperties
    ): YaXUnitExecutionResult = withContext(Dispatchers.IO) {
        val startTime = Instant.now()
        logger.info { "Starting YaXUnit test execution via ibcmd" }
        
        val duration = Duration.between(startTime, Instant.now())
        logger.warn { "ibcmd test execution not implemented yet" }
        
        YaXUnitExecutionResult(
            success = false,
            reportPath = null,
            exitCode = -1,
            standardOutput = "",
            errorOutput = "ibcmd test execution not implemented yet",
            duration = duration
        )
    }
}
