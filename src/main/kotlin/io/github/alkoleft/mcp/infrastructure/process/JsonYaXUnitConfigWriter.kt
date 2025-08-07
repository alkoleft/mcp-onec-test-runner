package io.github.alkoleft.mcp.infrastructure.process

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ArrayNode
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.YaXUnitConfigWriter
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private val logger = KotlinLogging.logger { }

/**
 * Реализация YaXUnitConfigWriter для создания JSON конфигураций запуска тестов
 * Поддерживает все параметры из документации YaXUnit
 */
class JsonYaXUnitConfigWriter : YaXUnitConfigWriter {
    
    private val objectMapper = ObjectMapper()
    
    override suspend fun writeConfig(
        request: TestExecutionRequest,
        outputPath: Path
    ): Path = withContext(Dispatchers.IO) {
        logger.debug { "Writing YaXUnit configuration to: $outputPath" }
        
        val config = createConfig(request)
        val jsonConfig = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config)
        
        // Создаем директорию если не существует
        Files.createDirectories(outputPath.parent)
        
        // Записываем конфигурацию в файл
        Files.write(outputPath, jsonConfig.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        
        logger.info { "YaXUnit configuration written successfully" }
        outputPath
    }
    
    override suspend fun createTempConfig(request: TestExecutionRequest): Path = withContext(Dispatchers.IO) {
        logger.debug { "Creating temporary YaXUnit configuration" }
        val tempFile = Files.createTempFile("yaxunit-config-", ".json")
        writeConfig(request, tempFile)
    }
    
    /**
     * Создает конфигурацию для запуска тестов
     */
    private fun createConfig(request: TestExecutionRequest): ObjectNode {
        val config = objectMapper.createObjectNode()
        
        logger.debug { "Creating configuration for request type: ${request.javaClass.simpleName}" }
        
        // Создаем фильтр в зависимости от типа запроса
        val filter = createFilter(request)
        if (filter.size() > 0) {
            config.set<ObjectNode>("filter", filter)
            logger.debug { "Added filter configuration" }
        }
        
        // Настройки отчета
        config.put("reportFormat", "jUnit")
        config.put("reportPath", determineReportPath(request))
        config.put("closeAfterTests", true)
        config.put("showReport", false)
        
        logger.debug { "Added report configuration: format=jUnit, path=${config.get("reportPath")}" }
        
        // Настройки логирования
        val logging = createLoggingConfig(request)
        config.set<ObjectNode>("logging", logging)
        
        logger.debug { "Added logging configuration" }
        
        return config
    }
    
    /**
     * Создает конфигурацию фильтра в зависимости от типа запроса
     */
    private fun createFilter(request: TestExecutionRequest): ObjectNode {
        val filter = objectMapper.createObjectNode()
        
        when (request) {
            is RunAllTestsRequest -> {
                // Для запуска всех тестов фильтр не нужен
                logger.debug { "No filter needed for RunAllTestsRequest" }
            }
            is RunModuleTestsRequest -> {
                // Фильтр по модулю
                val modulesArray = filter.putArray("modules")
                modulesArray.add(request.moduleName)
                logger.debug { "Added module filter: ${request.moduleName}" }
            }
            is RunListTestsRequest -> {
                // Фильтр по списку тестов
                val testsArray = filter.putArray("tests")
                request.testNames.forEach { testName ->
                    testsArray.add(testName)
                    logger.debug { "Added test filter: $testName" }
                }
            }
        }
        
        return filter
    }
    
    /**
     * Создает конфигурацию логирования
     */
    private fun createLoggingConfig(request: TestExecutionRequest): ObjectNode {
        val logging = objectMapper.createObjectNode()
        
        // Путь к файлу лога
        val logPath = request.testsPath.resolve("logs").resolve("tests.log")
        logging.put("file", logPath.toString())
        
        // Настройки вывода
        logging.put("console", false)
        logging.put("level", "info")
        
        // Дополнительные настройки логирования
        logging.put("includeTimestamp", true)
        logging.put("includeTestDetails", true)
        
        logger.debug { "Created logging configuration: file=$logPath, level=info" }
        
        return logging
    }
    
    /**
     * Определяет путь к отчету
     */
    private fun determineReportPath(request: TestExecutionRequest): String {
        val reportPath = request.testsPath.resolve("reports").resolve("junit.xml")
        logger.debug { "Determined report path: $reportPath" }
        return reportPath.toString()
    }
    
    /**
     * Создает конфигурацию с дополнительными параметрами
     */
    fun createConfigWithProperties(
        request: TestExecutionRequest,
        properties: ApplicationProperties
    ): ObjectNode {
        logger.debug { "Creating configuration with additional properties" }
        
        val config = createConfig(request)
        
        // Добавляем специфичные настройки из ApplicationProperties
        properties.testsPath?.let { testsPath ->
            if (!config.has("reportPath")) {
                val reportPath = testsPath.resolve("reports").resolve("junit.xml").toString()
                config.put("reportPath", reportPath)
                logger.debug { "Updated report path from properties: $reportPath" }
            }
        }
        
        // Добавляем настройки подключения если они есть
        properties.connection?.let { connection ->
            val connectionConfig = objectMapper.createObjectNode()
            connectionConfig.put("connectionString", connection.connectionString)
            
            config.set<ObjectNode>("connection", connectionConfig)
            logger.debug { "Added connection configuration" }
        }
        
        return config
    }
    
    /**
     * Создает расширенную конфигурацию с дополнительными параметрами
     */
    fun createExtendedConfig(
        request: TestExecutionRequest,
        properties: ApplicationProperties,
        additionalParams: Map<String, Any>
    ): ObjectNode {
        logger.debug { "Creating extended configuration with ${additionalParams.size} additional parameters" }
        
        val config = createConfigWithProperties(request, properties)
        
        // Добавляем дополнительные параметры
        additionalParams.forEach { (key, value) ->
            when (value) {
                is String -> config.put(key, value)
                is Int -> config.put(key, value)
                is Boolean -> config.put(key, value)
                is Double -> config.put(key, value)
                is List<*> -> {
                    val array = config.putArray(key)
                    value.forEach { item ->
                        when (item) {
                            is String -> array.add(item)
                            is Int -> array.add(item)
                            is Boolean -> array.add(item)
                            is Double -> array.add(item)
                        }
                    }
                }
            }
            logger.debug { "Added additional parameter: $key = $value" }
        }
        
        return config
    }
    
    /**
     * Валидирует конфигурацию
     */
    fun validateConfig(config: ObjectNode): Boolean {
        logger.debug { "Validating YaXUnit configuration" }
        
        // Проверяем обязательные поля
        val requiredFields = listOf("reportFormat", "reportPath", "closeAfterTests", "showReport", "logging")
        val missingFields = requiredFields.filter { !config.has(it) }
        
        if (missingFields.isNotEmpty()) {
            logger.warn { "Missing required fields in configuration: ${missingFields.joinToString(", ")}" }
            return false
        }
        
        // Проверяем формат отчета
        val reportFormat = config.get("reportFormat").asText()
        if (reportFormat != "jUnit") {
            logger.warn { "Unsupported report format: $reportFormat, only jUnit is supported" }
            return false
        }
        
        logger.debug { "Configuration validation passed" }
        return true
    }
}
