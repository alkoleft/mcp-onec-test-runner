package io.github.alkoleft.mcp.infrastructure.process

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.YaXUnitConfigWriter
import io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitConfig
import io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitConfigBuilderImpl
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
    private fun createConfig(request: TestExecutionRequest): YaXUnitConfig {
        logger.debug { "Creating configuration for request type: ${request.javaClass.simpleName}" }

        // Создаем конфигурацию с помощью построителя
        val config = configBuilder.createFromRequest(request)

        // Валидируем конфигурацию
        val validationResult = configBuilder.validate()
        if (!validationResult.isValid) {
            logger.warn { "Configuration validation failed: ${validationResult.errors.joinToString(", ")}" }
        }

        return config
    }
}
