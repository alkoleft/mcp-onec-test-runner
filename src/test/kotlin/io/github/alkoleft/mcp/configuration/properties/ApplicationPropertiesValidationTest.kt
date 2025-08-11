package io.github.alkoleft.mcp.configuration.properties

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

class ApplicationPropertiesValidationTest {
    @Test
    fun `should validate correct configuration`() {
        val tempDir = Files.createTempDirectory("test")
        val configPath = tempDir.resolve("config")
        Files.createDirectory(configPath)

        val properties =
            ApplicationProperties(
                basePath = tempDir,
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "config",
                                    name = "Configuration",
                                    type = SourceSetType.CONFIGURATION,
                                ),
                            ),
                    ),
                connection =
                    ConnectionProperties(
                        connectionString = "File=test.db;",
                    ),
                platformVersion = "8.3.20.1234",
                tools = ToolsProperties(BuilderType.DESIGNER),
            )

        // Если валидация прошла успешно, тест не должен выбросить исключение
        assertEquals(tempDir, properties.basePath)
        assertEquals("8.3.20.1234", properties.platformVersion)
    }

    @Test
    fun `should throw exception for non-existent base path`() {
        val nonExistentPath = Path.of("/non/existent/path")

        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = nonExistentPath,
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "config",
                                    name = "Configuration",
                                    type = SourceSetType.CONFIGURATION,
                                ),
                            ),
                    ),
                connection = ConnectionProperties(connectionString = "File=test.db;"),
            )
        }
    }

    @Test
    fun `should throw exception for empty source set`() {
        val tempDir = Files.createTempDirectory("test")

        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = tempDir,
                sourceSet = SourceSet(),
                connection = ConnectionProperties(connectionString = "File=test.db;"),
            )
        }
    }

    @Test
    fun `should throw exception for missing configuration source set`() {
        val tempDir = Files.createTempDirectory("test")

        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = tempDir,
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "extensions",
                                    name = "Extensions",
                                    type = SourceSetType.EXTENSION,
                                ),
                            ),
                    ),
                connection = ConnectionProperties(connectionString = "File=test.db;"),
            )
        }
    }

    @Test
    fun `should throw exception for empty connection string`() {
        val tempDir = Files.createTempDirectory("test")
        val configPath = tempDir.resolve("config")
        Files.createDirectory(configPath)

        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = tempDir,
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "config",
                                    name = "Configuration",
                                    type = SourceSetType.CONFIGURATION,
                                ),
                            ),
                    ),
                connection = ConnectionProperties(connectionString = ""),
            )
        }
    }

    @Test
    fun `should throw exception for invalid platform version`() {
        val tempDir = Files.createTempDirectory("test")
        val configPath = tempDir.resolve("config")
        Files.createDirectory(configPath)

        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = tempDir,
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "config",
                                    name = "Configuration",
                                    type = SourceSetType.CONFIGURATION,
                                ),
                            ),
                    ),
                connection = ConnectionProperties(connectionString = "File=test.db;"),
                platformVersion = "invalid-version",
            )
        }
    }

    @Test
    fun `should validate source set item with invalid path`() {
        assertThrows<IllegalArgumentException> {
            SourceSetItem(
                path = "",
                name = "Test",
                type = SourceSetType.CONFIGURATION,
            )
        }
    }

    @Test
    fun `should validate source set item with invalid name`() {
        assertThrows<IllegalArgumentException> {
            SourceSetItem(
                path = "config",
                name = "",
                type = SourceSetType.CONFIGURATION,
            )
        }
    }
}
