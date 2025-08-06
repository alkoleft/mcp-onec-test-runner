package io.github.alkoleft.mcp.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Files
import java.nio.file.Path

/**
 * Настройки приложения с валидацией конфигурации
 */
@ConfigurationProperties(prefix = "app")
data class ApplicationProperties(
    val basePath: Path,
    val sourceSet: SourceSet,
    val connection: ConnectionProperties,
    val platformVersion: String? = null,
    val tools: ToolsProperties
) {
    init {
        validateConfiguration()
    }

    /**
     * Валидирует конфигурацию приложения
     */
    private fun validateConfiguration() {
        require(Files.exists(basePath)) { "Base path does not exist: $basePath" }
        require(sourceSet.isNotEmpty()) { "Source set cannot be empty" }
        require(connection.connectionString.isNotBlank()) { "Connection string cannot be empty" }
        require(sourceSet.any { it.type == SourceSetType.CONFIGURATION }) { "Configuration source set is required" }
    }

    // Упрощенные вычисляемые свойства с lazy инициализацией
    val configurationPath: Path? by lazy {
        sourceSet.find { it.type == SourceSetType.CONFIGURATION }
            ?.let { basePath.resolve(it.path) }
    }

    val testsPath: Path? by lazy {
        sourceSet.find { it.purpose.contains(SourceSetPurpose.TESTS) }
            ?.let { basePath.resolve(it.path) }
    }

    val yaxunitEnginePath: Path? by lazy {
        sourceSet.find { it.purpose.contains(SourceSetPurpose.YAXUNIT) }
            ?.let { basePath.resolve(it.path) }
    }

    val mainCodePath: Path? by lazy {
        sourceSet.find { it.purpose.contains(SourceSetPurpose.MAIN) }
            ?.let { basePath.resolve(it.path) }
    }

    val extensions: List<String> by lazy {
        sourceSet.filter { it.type == SourceSetType.EXTENSION }
            .map { it.name }
    }
}