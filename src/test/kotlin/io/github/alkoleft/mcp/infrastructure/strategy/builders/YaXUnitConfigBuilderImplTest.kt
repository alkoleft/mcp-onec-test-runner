package io.github.alkoleft.mcp.infrastructure.strategy.builders

import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.infrastructure.yaxunit.ConnectionConfig
import io.github.alkoleft.mcp.infrastructure.yaxunit.LoggingConfig
import io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitConfigBuilderImpl
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YaXUnitConfigBuilderImplTest {

    private val builder = YaXUnitConfigBuilderImpl()

    @Test
    fun `should build configuration with module filter`() {
        // Given
        val modules = listOf("Module1", "Module2")

        // When
        val config = builder
            .withModuleFilter(modules)
            .withReportFormat("jUnit")
            .withReportPath(Path.of("/reports/junit.xml"))
            .build()

        // Then
        assertNotNull(config)
        assertEquals("jUnit", config.reportFormat)
        assertEquals(Path.of("/reports/junit.xml"), config.reportPath)
        assertTrue(config.closeAfterTests)
        assertTrue(!config.showReport)

        val filter = config.filter
        assertNotNull(filter)
        assertEquals(modules, filter.modules)
        assertTrue(filter.tests.isEmpty())
    }

    @Test
    fun `should build configuration with test filter`() {
        // Given
        val tests = listOf("Test1", "Test2", "Test3")

        // When
        val config = builder
            .withTestFilter(tests)
            .withReportFormat("jUnit")
            .build()

        // Then
        assertNotNull(config)
        assertEquals("jUnit", config.reportFormat)

        val filter = config.filter
        assertNotNull(filter)
        assertEquals(tests, filter.tests)
        assertTrue(filter.modules.isEmpty())
    }

    @Test
    fun `should build configuration with logging`() {
        // Given
        val logging = LoggingConfig(
            file = Path.of("/logs/tests.log"),
            console = true,
            level = "debug"
        )

        // When
        val config = builder
            .withLogging(logging)
            .build()

        // Then
        assertNotNull(config)
        assertEquals(logging, config.logging)
    }

    @Test
    fun `should build configuration with connection`() {
        // Given
        val connection = ConnectionConfig(
            connectionString = "File=/path/to/database",
            username = "admin",
            password = "password",
            timeout = 30
        )

        // When
        val config = builder
            .withConnection(connection)
            .build()

        // Then
        assertNotNull(config)
        assertEquals(connection, config.connection)
    }

    @Test
    fun `should build configuration with additional parameters`() {
        // Given
        val additionalParams = mapOf(
            "param1" to "value1",
            "param2" to 42,
            "param3" to true,
            "param4" to 3.14
        )

        // When
        val config = builder
            .withParameter("param1", "value1")
            .withParameter("param2", 42)
            .withParameter("param3", true)
            .withParameter("param4", 3.14)
            .build()

        // Then
        assertNotNull(config)
        assertEquals(additionalParams, config.additionalParameters)
    }

    @Test
    fun `should create configuration from RunAllTestsRequest`() {
        // Given
        val request = RunAllTestsRequest(
            projectPath = Path.of("/project"),
            testsPath = Path.of("/project/tests"),
            ibConnection = "File=/path/to/database",
            platformVersion = "8.3.20.1234"
        )

        // When
        val config = builder.createFromRequest(request)

        // Then
        assertNotNull(config)
        assertEquals("jUnit", config.reportFormat)
        assertEquals(Path.of("/project/tests/reports/junit.xml"), config.reportPath)
        assertTrue(config.closeAfterTests)
        assertTrue(!config.showReport)

        val logging = config.logging
        assertEquals(Path.of("/project/tests/logs/tests.log"), logging.file)
        assertTrue(!logging.console)
        assertEquals("info", logging.level)

        val connection = config.connection
        assertNotNull(connection)
        assertEquals("File=/path/to/database", connection.connectionString)
    }

    @Test
    fun `should create configuration from RunModuleTestsRequest`() {
        // Given
        val request = RunModuleTestsRequest(
            projectPath = Path.of("/project"),
            testsPath = Path.of("/project/tests"),
            ibConnection = "File=/path/to/database",
            platformVersion = "8.3.20.1234",
            moduleName = "TestModule"
        )

        // When
        val config = builder.createFromRequest(request)

        // Then
        assertNotNull(config)
        val filter = config.filter
        assertNotNull(filter)
        assertEquals(listOf("TestModule"), filter.modules)
        assertTrue(filter.tests.isEmpty())
    }

    @Test
    fun `should create configuration from RunListTestsRequest`() {
        // Given
        val request = RunListTestsRequest(
            projectPath = Path.of("/project"),
            testsPath = Path.of("/project/tests"),
            ibConnection = "File=/path/to/database",
            platformVersion = "8.3.20.1234",
            testNames = listOf("Test1", "Test2", "Test3")
        )

        // When
        val config = builder.createFromRequest(request)

        // Then
        assertNotNull(config)
        val filter = config.filter
        assertNotNull(filter)
        assertEquals(listOf("Test1", "Test2", "Test3"), filter.tests)
        assertTrue(filter.modules.isEmpty())
    }

    @Test
    fun `should validate configuration successfully`() {
        // Given
        val config = builder
            .withReportFormat("jUnit")
            .withLogging(LoggingConfig(console = true))
            .build()

        // When
        val validationResult = builder.validate()

        // Then
        assertTrue(validationResult.isValid)
        assertTrue(validationResult.errors.isEmpty())
    }

    @Test
    fun `should validate configuration with errors`() {
        // Given
        val config = builder
            .withReportFormat("invalid")
            .build()

        // When
        val validationResult = builder.validate()

        // Then
        assertTrue(!validationResult.isValid)
        assertTrue(validationResult.errors.isNotEmpty())
        assertTrue(validationResult.errors.any { it.contains("Unsupported report format") })
    }
}
