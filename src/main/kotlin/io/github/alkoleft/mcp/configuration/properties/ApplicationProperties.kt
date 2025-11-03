package io.github.alkoleft.mcp.configuration.properties

import io.github.alkoleft.mcp.core.modules.TEST_PATH
import io.github.alkoleft.mcp.infrastructure.storage.calculateStringHash
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
    val workPath: Path by lazy {
        val path =
            if (id != null && id.isNotBlank()) {
                id
            } else {
                calculateStringHash(basePath.toString())
            }
        Path.of(System.getProperty("java.io.tmpdir"), "mcp-yaxunit-runner", path)
    }

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
        require(basePath.toString().isNotBlank()) { "Базовый путь не может быть пустым" }
        require(Files.exists(basePath)) { "Базовый путь не существует: $basePath" }
        require(Files.isDirectory(basePath)) { "Базовый путь должен быть директорией: $basePath" }
        require(Files.isReadable(basePath)) { "Базовый путь должен быть доступен для чтения: $basePath" }
    }

    /**
     * Валидация набора исходного кода
     */
    private fun validateSourceSet() {
        require(sourceSet.isNotEmpty()) { "Набор исходников не может быть пустым" }

        sourceSet.forEach { item ->
            validateSourceSetItem(item)
        }

        // Проверяем наличие обязательных элементов
        require(sourceSet.any { it.type == SourceSetType.CONFIGURATION }) {
            "Требуется source set типа CONFIGURATION"
        }

        // Проверяем уникальность путей
        val paths = sourceSet.map { it.path }
        require(paths.size == paths.toSet().size) {
            "Пути в source set должны быть уникальными"
        }
    }

    /**
     * Валидация элемента набора исходного кода
     */
    private fun validateSourceSetItem(item: SourceSetItem) {
        require(item.path.isNotBlank()) { "Путь элемента source set не может быть пустым" }
        require(item.name.isNotBlank()) { "Имя элемента source set не может быть пустым" }

        // Проверяем, что путь существует относительно basePath
        val fullPath = basePath.resolve(item.path)
        require(Files.exists(fullPath)) {
            "Путь source set не существует: ${item.path}"
        }
    }

    /**
     * Валидация настроек подключения
     */
    private fun validateConnection() {
        require(connection.connectionString.isNotBlank()) {
            "Строка подключения не может быть пустой"
        }

        // Проверяем формат строки подключения
        require(connection.connectionString.contains("=")) {
            "Строка подключения должна содержать символ '='"
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
                "Версия платформы должна быть в формате x.x.x.x: $platformVersion"
            }
        }
    }

    /**
     * Валидация настроек инструментов
     */
    private fun validateTools() {
        // Проверяем, что builder имеет допустимое значение
        require(tools.builder in BuilderType.entries.toTypedArray()) {
            "Недопустимый тип сборщика: ${tools.builder}"
        }
    }

    val testsPath: Path by lazy {
        sourceSet
            .find { it.purpose.contains(SourceSetPurpose.TESTS) }
            ?.let { basePath.resolve(it.path) }
            ?: basePath.resolve(TEST_PATH)
    }
}
