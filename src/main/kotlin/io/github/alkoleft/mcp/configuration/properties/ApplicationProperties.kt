package io.github.alkoleft.mcp.configuration.properties

import io.github.alkoleft.mcp.core.modules.TEST_PATH
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Files
import java.nio.file.Path

/**
 * Настройки приложения с валидацией конфигурации
 */
@ConfigurationProperties(prefix = "app")
data class ApplicationProperties(
    val id: String? = null,
    val format: ProjectFormat = ProjectFormat.DESIGNER,
    val basePath: Path,
    val sourceSet: SourceSet = SourceSet(),
    val connection: ConnectionProperties = ConnectionProperties(),
    val platformVersion: String = "",
    val tools: ToolsProperties = ToolsProperties(),
) {
    init {
        validateConfiguration()
    }

    /**
     * Валидирует конфигурацию приложения
     */
    private fun validateConfiguration() {
        validateBasePath()
        validateSourceSet()
        validateConnection()
        validatePlatformVersion()
        validateTools()
    }

    /**
     * Валидация базового пути
     */
    private fun validateBasePath() {
        require(basePath.toString().isNotBlank()) { "Base path cannot be empty" }
        require(Files.exists(basePath)) { "Base path does not exist: $basePath" }
        require(Files.isDirectory(basePath)) { "Base path must be a directory: $basePath" }
        require(Files.isReadable(basePath)) { "Base path must be readable: $basePath" }
    }

    /**
     * Валидация набора исходного кода
     */
    private fun validateSourceSet() {
        require(sourceSet.isNotEmpty()) { "Source set cannot be empty" }

        sourceSet.forEach { item ->
            validateSourceSetItem(item)
        }

        // Проверяем наличие обязательных элементов
        require(sourceSet.any { it.type == SourceSetType.CONFIGURATION }) {
            "Configuration source set is required"
        }

        // Проверяем уникальность путей
        val paths = sourceSet.map { it.path }
        require(paths.size == paths.toSet().size) {
            "Source set paths must be unique"
        }
    }

    /**
     * Валидация элемента набора исходного кода
     */
    private fun validateSourceSetItem(item: SourceSetItem) {
        require(item.path.isNotBlank()) { "Source set item path cannot be empty" }
        require(item.name.isNotBlank()) { "Source set item name cannot be empty" }

        // Проверяем, что путь существует относительно basePath
        val fullPath = basePath.resolve(item.path)
        require(Files.exists(fullPath)) {
            "Source set path does not exist: ${item.path}"
        }
    }

    /**
     * Валидация настроек подключения
     */
    private fun validateConnection() {
        require(connection.connectionString.isNotBlank()) {
            "Connection string cannot be empty"
        }

        // Проверяем формат строки подключения
        require(connection.connectionString.contains("=")) {
            "Connection string must contain '=' character"
        }
    }

    /**
     * Валидация версии платформы
     */
    private fun validatePlatformVersion() {
        if (platformVersion.isNotBlank()) {
            // Простая проверка формата версии (x.x.x.x)
            val versionPattern = Regex("^\\d+(\\.\\d+)*$")
            require(versionPattern.matches(platformVersion)) {
                "Platform version must be in format x.x.x.x: $platformVersion"
            }
        }
    }

    /**
     * Валидация настроек инструментов
     */
    private fun validateTools() {
        // Проверяем, что builder имеет допустимое значение
        require(tools.builder in BuilderType.entries.toTypedArray()) {
            "Invalid builder type: ${tools.builder}"
        }
    }

    // Упрощенные вычисляемые свойства с lazy инициализацией
    val configurationPath: Path? by lazy {
        sourceSet
            .find { it.type == SourceSetType.CONFIGURATION }
            ?.let { basePath.resolve(it.path) }
    }

    val testsPath: Path by lazy {
        sourceSet
            .find { it.purpose.contains(SourceSetPurpose.TESTS) }
            ?.let { basePath.resolve(it.path) }
            ?: basePath.resolve(TEST_PATH)
    }

    val yaxunitEnginePath: Path? by lazy {
        sourceSet
            .find { it.purpose.contains(SourceSetPurpose.YAXUNIT) }
            ?.let { basePath.resolve(it.path) }
    }

    val mainCodePath: Path? by lazy {
        sourceSet
            .find { it.purpose.contains(SourceSetPurpose.MAIN) }
            ?.let { basePath.resolve(it.path) }
    }

    val extensions: List<String> by lazy {
        sourceSet
            .filter { it.type == SourceSetType.EXTENSION }
            .map { it.name }
    }
}
