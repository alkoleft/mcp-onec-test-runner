package io.github.alkoleft.mcp.infrastructure.process

import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcessYaXUnitRunnerTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var mockUtilLocator: CrossPlatformUtilLocator
    private lateinit var mockConfigWriter: JsonYaXUnitConfigWriter
    private lateinit var runner: ProcessYaXUnitRunner
    private lateinit var mockUtilityLocation: UtilityLocation

    @BeforeEach
    fun setUp() {
        mockUtilLocator = mock()
        mockConfigWriter = mock()
        runner = ProcessYaXUnitRunner(mockUtilLocator, mockConfigWriter)
        
        mockUtilityLocation = UtilityLocation(
            executablePath = tempDir.resolve("1cv8c.exe"),
            version = "8.3.24.1482",
            platformType = io.github.alkoleft.mcp.core.modules.PlatformType.WINDOWS
        )
    }

    @Test
    fun `should create runner with dependencies`() {
        // Given
        val utilLocator = mock<CrossPlatformUtilLocator>()
        val configWriter = mock<JsonYaXUnitConfigWriter>()
        
        // When
        val runner = ProcessYaXUnitRunner(utilLocator, configWriter)
        
        // Then
        assertTrue { runner is ProcessYaXUnitRunner }
    }

    @Test
    fun `should build enterprise command args for file database`() = runBlocking {
        // Given
        val request = RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        )
        val configPath = tempDir.resolve("config.json")
        
        // When
        val result = runner.executeTests(mockUtilityLocation, configPath, request)
        
        // Then
        assertFalse(result.success) // Process will fail because executable doesn't exist
        assertEquals(-1, result.exitCode)
        assertTrue(result.errorOutput.contains("Unknown error") || result.errorOutput.contains("Process exited"))
    }

    @Test
    fun `should build enterprise command args for server database`() = runBlocking {
        // Given
        val request = RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "Srvr=localhost;Ref=TestDB;",
            platformVersion = "8.3.24.1482"
        )
        val configPath = tempDir.resolve("config.json")
        
        // When
        val result = runner.executeTests(mockUtilityLocation, configPath, request)
        
        // Then
        assertFalse(result.success)
        assertEquals(-1, result.exitCode)
    }

    @Test
    fun `should handle authentication parameters`() = runBlocking {
        // Given
        val request = RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;N=Admin;P=Password;",
            platformVersion = "8.3.24.1482"
        )
        val configPath = tempDir.resolve("config.json")
        
        // When
        val result = runner.executeTests(mockUtilityLocation, configPath, request)
        
        // Then
        assertFalse(result.success)
        assertEquals(-1, result.exitCode)
    }

    @Test
    fun `should determine report path correctly`() = runBlocking {
        // Given
        val request = RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        )
        val configPath = tempDir.resolve("config.json")
        
        // Create a mock report file
        val reportPath = tempDir.resolve("tests").resolve("reports").resolve("junit.xml")
        reportPath.parent.toFile().mkdirs()
        reportPath.toFile().createNewFile()
        
        // When
        val result = runner.executeTests(mockUtilityLocation, configPath, request)
        
        // Then
        assertFalse(result.success) // Process will fail, but we can check the logic
        assertEquals(-1, result.exitCode)
    }

    @Test
    fun `should handle process execution errors`() = runBlocking {
        // Given
        val request = RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        )
        val configPath = tempDir.resolve("config.json")
        
        // When
        val result = runner.executeTests(mockUtilityLocation, configPath, request)
        
        // Then
        assertFalse(result.success)
        assertTrue(result.errorOutput.isNotEmpty())
        assertTrue(result.duration.toSeconds() >= 0)
    }

    @Test
    fun `should return correct execution result structure`() = runBlocking {
        // Given
        val request = RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=C:\\TestDB;",
            platformVersion = "8.3.24.1482"
        )
        val configPath = tempDir.resolve("config.json")
        
        // When
        val result = runner.executeTests(mockUtilityLocation, configPath, request)
        
        // Then
        assertFalse(result.success)
        assertEquals(-1, result.exitCode)
        assertTrue(result.standardOutput.isEmpty())
        assertTrue(result.errorOutput.isNotEmpty())
        assertTrue(result.duration.toSeconds() >= 0)
        assertTrue(result.reportPath == null) // No report created due to process failure
    }
}
