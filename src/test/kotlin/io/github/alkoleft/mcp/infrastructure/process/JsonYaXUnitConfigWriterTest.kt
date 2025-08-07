package io.github.alkoleft.mcp.infrastructure.process

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonYaXUnitConfigWriterTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var configWriter: JsonYaXUnitConfigWriter
    private lateinit var properties: ApplicationProperties

    @BeforeEach
    fun setUp() {
        configWriter = JsonYaXUnitConfigWriter()
        properties = ApplicationProperties(
            basePath = tempDir,
            platformVersion = "8.3.24.1482",
            connection = ConnectionProperties(
                connectionString = "File=C:\\TestDB;"
            )
        )
    }

    @Test
    fun `should create config writer`() {
        // Given & When
        val configWriter = JsonYaXUnitConfigWriter()
        
        // Then
        assertTrue { configWriter is JsonYaXUnitConfigWriter }
    }

    @Test
    fun `should create temp config for RunAllTestsRequest`() = runBlocking {
        // Given
        val request = RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        )
        
        // When
        val configPath = configWriter.createTempConfig(request)
        
        // Then
        assertTrue(configPath.toFile().exists())
        assertTrue(configPath.toString().endsWith(".json"))
    }

    @Test
    fun `should create config for RunModuleTestsRequest with module filter`() = runBlocking {
        // Given
        val request = RunModuleTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482",
            moduleName = "TestModule"
        )
        
        // When
        val configPath = configWriter.createTempConfig(request)
        val configContent = configPath.toFile().readText()
        
        // Then
        assertTrue(configContent.contains("TestModule"))
        assertTrue(configContent.contains("modules"))
        assertTrue(configContent.contains("jUnit"))
    }

    @Test
    fun `should create config for RunListTestsRequest with test list`() = runBlocking {
        // Given
        val request = RunListTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482",
            testNames = listOf("Test1", "Test2", "Test3")
        )
        
        // When
        val configPath = configWriter.createTempConfig(request)
        val configContent = configPath.toFile().readText()
        
        // Then
        assertTrue(configContent.contains("Test1"))
        assertTrue(configContent.contains("Test2"))
        assertTrue(configContent.contains("Test3"))
        assertTrue(configContent.contains("tests"))
        assertTrue(configContent.contains("jUnit"))
    }

    @Test
    fun `should create config with properties`() = runBlocking {
        // Given
        val request = RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        )
        
        // When
        val config = configWriter.createConfigWithProperties(request, properties)
        
        // Then
        assertTrue(config.has("reportFormat"))
        assertTrue(config.has("reportPath"))
        assertTrue(config.has("closeAfterTests"))
        assertTrue(config.has("showReport"))
        assertTrue(config.has("logging"))
        assertTrue(config.has("connection"))
        
        assertEquals("jUnit", config.get("reportFormat").asText())
        assertTrue(config.get("closeAfterTests").asBoolean())
        assertFalse(config.get("showReport").asBoolean())
    }

    @Test
    fun `should create extended config with additional parameters`() = runBlocking {
        // Given
        val request = RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        )
        
        val additionalParams = mapOf(
            "timeout" to 300,
            "parallel" to true,
            "retryCount" to 3,
            "customParam" to "customValue"
        )
        
        // When
        val config = configWriter.createExtendedConfig(request, properties, additionalParams)
        
        // Then
        assertTrue(config.has("timeout"))
        assertTrue(config.has("parallel"))
        assertTrue(config.has("retryCount"))
        assertTrue(config.has("customParam"))
        
        assertEquals(300, config.get("timeout").asInt())
        assertTrue(config.get("parallel").asBoolean())
        assertEquals(3, config.get("retryCount").asInt())
        assertEquals("customValue", config.get("customParam").asText())
    }

    @Test
    fun `should validate correct config`() {
        // Given
        val config = configWriter.createConfigWithProperties(RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        ), properties)
        
        // When
        val isValid = configWriter.validateConfig(config)
        
        // Then
        assertTrue(isValid)
    }

    @Test
    fun `should reject config with missing required fields`() {
        // Given
        val config = configWriter.createConfigWithProperties(RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        ), properties)
        config.remove("reportFormat")
        
        // When
        val isValid = configWriter.validateConfig(config)
        
        // Then
        assertFalse(isValid)
    }

    @Test
    fun `should reject config with wrong report format`() {
        // Given
        val config = configWriter.createConfigWithProperties(RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        ), properties)
        config.put("reportFormat", "html")
        
        // When
        val isValid = configWriter.validateConfig(config)
        
        // Then
        assertFalse(isValid)
    }

    @Test
    fun `should write config to file`() = runBlocking {
        // Given
        val request = RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        )
        val outputPath = tempDir.resolve("test-config.json")
        
        // When
        val writtenPath = configWriter.writeConfig(request, outputPath)
        
        // Then
        assertTrue(writtenPath.toFile().exists())
        assertEquals(outputPath, writtenPath)
        
        val content = writtenPath.toFile().readText()
        assertTrue(content.contains("jUnit"))
        assertTrue(content.contains("reportPath"))
        assertTrue(content.contains("logging"))
    }
}
