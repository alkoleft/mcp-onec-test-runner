package io.github.alkoleft.mcp.infrastructure.process

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.YaXUnitConfigWriter
import io.github.alkoleft.mcp.core.modules.strategy.YaXUnitConfig
import io.github.alkoleft.mcp.infrastructure.strategy.builders.YaXUnitConfigBuilderImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private val logger = KotlinLogging.logger { }

/**
 * Реализация YaXUnitConfigWriter для создания JSON конфигураций запуска тестов
 * Поддерживает все параметры из документации YaXUnit
 * Интегрирован с построителем конфигурации
 */
class JsonYaXUnitConfigWriter : YaXUnitConfigWriter {
    
    private val objectMapper = ObjectMapper()
    private val configBuilder = YaXUnitConfigBuilderImpl()
    
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
        logger.debug { "Creating configuration for request type: ${request.javaClass.simpleName}" }

        // Создаем конфигурацию с помощью построителя
        val config = configBuilder.createFromRequest(request)

        // Валидируем конфигурацию
        val validationResult = configBuilder.validate()
        if (!validationResult.isValid) {
            logger.warn { "Configuration validation failed: ${validationResult.errors.joinToString(", ")}" }
        }

        // Конвертируем в JSON
        return convertConfigToJson(config)
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

            config.set<JsonNode>("connection", connectionConfig)
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
     * Конвертирует конфигурацию в JSON формат
     */
    private fun convertConfigToJson(config: YaXUnitConfig): ObjectNode {
        val jsonConfig = objectMapper.createObjectNode()

        // Добавляем фильтр если есть
        config.filter?.let { filter ->
            val filterNode = objectMapper.createObjectNode()
            if (filter.modules.isNotEmpty()) {
                val modulesArray = filterNode.putArray("modules")
                filter.modules.forEach { modulesArray.add(it) }
            }
            if (filter.tests.isNotEmpty()) {
                val testsArray = filterNode.putArray("tests")
                filter.tests.forEach { testsArray.add(it) }
            }
            jsonConfig.set<JsonNode>("filter", filterNode)
        }

        // Добавляем настройки отчета
        jsonConfig.put("reportFormat", config.reportFormat)
        config.reportPath?.let { jsonConfig.put("reportPath", it.toString()) }
        jsonConfig.put("closeAfterTests", config.closeAfterTests)
        jsonConfig.put("showReport", config.showReport)

        // Добавляем настройки логирования
        val loggingNode = objectMapper.createObjectNode()
        config.logging.file?.let { loggingNode.put("file", it.toString()) }
        loggingNode.put("console", config.logging.console)
        loggingNode.put("level", config.logging.level)
        jsonConfig.set<JsonNode>("logging", loggingNode)

        return jsonConfig
    }

    /**
     * Валидирует конфигурацию
     */
    fun validateConfig(config: ObjectNode): Boolean {
        logger.debug { "Validating YaXUnit configuration" }
        
        // Проверяем обязательные поля
        val requiredFields = listOf("reportFormat", "closeAfterTests", "showReport", "logging")
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
