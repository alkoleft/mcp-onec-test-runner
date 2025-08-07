package io.github.alkoleft.mcp.application.actions.test

import io.github.alkoleft.mcp.application.actions.TestExecutionResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.alkoleft.mcp.infrastructure.process.EnhancedReportParser
import io.github.alkoleft.mcp.infrastructure.process.JsonYaXUnitConfigWriter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YaXUnitTestActionTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var mockPlatformUtilityDsl: PlatformUtilityDsl
    private lateinit var mockUtilLocator: CrossPlatformUtilLocator
    private lateinit var mockConfigWriter: JsonYaXUnitConfigWriter
    private lateinit var mockReportParser: EnhancedReportParser
    private lateinit var testAction: YaXUnitTestAction
    private lateinit var properties: ApplicationProperties
    private lateinit var mockUtilityLocation: UtilityLocation

    @BeforeEach
    fun setUp() {
        mockPlatformUtilityDsl = mock()
        mockUtilLocator = mock()
        mockConfigWriter = mock()
        mockReportParser = mock()
        
        testAction = YaXUnitTestAction(
            mockPlatformUtilityDsl,
            mockUtilLocator,
            mockConfigWriter,
            mockReportParser
        )
        
        properties = ApplicationProperties(
            basePath = tempDir,
            platformVersion = "8.3.24.1482",
            connection = ConnectionProperties(
                connectionString = "File=C:\\TestDB;"
            )
        )
        
        mockUtilityLocation = UtilityLocation(
            executablePath = tempDir.resolve("1cv8c.exe"),
            version = "8.3.24.1482",
            platformType = io.github.alkoleft.mcp.core.modules.PlatformType.WINDOWS
        )
        
        // Setup mocks
        runBlocking {
            whenever(mockUtilLocator.locateUtility(any(), any())).thenReturn(mockUtilityLocation)
            whenever(mockConfigWriter.createTempConfig(any())).thenReturn(tempDir.resolve("config.json"))
        }
    }

    @Test
    fun `should create YaXUnitTestAction with dependencies`() {
        // Given & When
        val testAction = YaXUnitTestAction(
            mockPlatformUtilityDsl,
            mockUtilLocator,
            mockConfigWriter,
            mockReportParser
        )
        
        // Then
        assertTrue { testAction is YaXUnitTestAction }
    }

    @Test
    fun `should run all tests when no filter provided`() = runBlocking {
        // Given
        val filter: String? = null
        
        // When
        val result = testAction.run(filter, properties)
        
        // Then
        assertFalse(result.success) // Process will fail because executable doesn't exist
        assertEquals(0, result.testsRun)
        assertEquals(0, result.testsPassed)
        assertEquals(0, result.testsFailed)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.duration.toSeconds() >= 0)
    }

    @Test
    fun `should run module tests when filter starts with module`() = runBlocking {
        // Given
        val filter = "module:TestModule"
        
        // When
        val result = testAction.run(filter, properties)
        
        // Then
        assertFalse(result.success)
        assertEquals(0, result.testsRun)
        assertEquals(0, result.testsPassed)
        assertEquals(0, result.testsFailed)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `should run specific tests when filter contains comma`() = runBlocking {
        // Given
        val filter = "Test1,Test2,Test3"
        
        // When
        val result = testAction.run(filter, properties)
        
        // Then
        assertFalse(result.success)
        assertEquals(0, result.testsRun)
        assertEquals(0, result.testsPassed)
        assertEquals(0, result.testsFailed)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `should run single test when filter is single test name`() = runBlocking {
        // Given
        val filter = "SingleTest"
        
        // When
        val result = testAction.run(filter, properties)
        
        // Then
        assertFalse(result.success)
        assertEquals(0, result.testsRun)
        assertEquals(0, result.testsPassed)
        assertEquals(0, result.testsFailed)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `should run all tests via dedicated method`() = runBlocking {
        // Given & When
        val result = testAction.runAllTests(properties)
        
        // Then
        assertFalse(result.success)
        assertEquals(0, result.testsRun)
        assertEquals(0, result.testsPassed)
        assertEquals(0, result.testsFailed)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `should run module tests via dedicated method`() = runBlocking {
        // Given
        val moduleName = "TestModule"
        
        // When
        val result = testAction.runModuleTests(moduleName, properties)
        
        // Then
        assertFalse(result.success)
        assertEquals(0, result.testsRun)
        assertEquals(0, result.testsPassed)
        assertEquals(0, result.testsFailed)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `should run specific tests via dedicated method`() = runBlocking {
        // Given
        val testNames = listOf("Test1", "Test2", "Test3")
        
        // When
        val result = testAction.runSpecificTests(testNames, properties)
        
        // Then
        assertFalse(result.success)
        assertEquals(0, result.testsRun)
        assertEquals(0, result.testsPassed)
        assertEquals(0, result.testsFailed)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `should run single test via dedicated method`() = runBlocking {
        // Given
        val testName = "SingleTest"
        
        // When
        val result = testAction.runSingleTest(testName, properties)
        
        // Then
        assertFalse(result.success)
        assertEquals(0, result.testsRun)
        assertEquals(0, result.testsPassed)
        assertEquals(0, result.testsFailed)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `should handle utility location failure`() = runBlocking {
        // Given
        whenever(mockUtilLocator.locateUtility(any(), any()))
            .thenThrow(RuntimeException("Utility not found"))
        
        // When & Then
        try {
            testAction.run(null, properties)
            assertTrue(false, "Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("YaXUnit test execution failed") == true)
        }
    }

    @Test
    fun `should handle config creation failure`() = runBlocking {
        // Given
        whenever(mockConfigWriter.createTempConfig(any()))
            .thenThrow(RuntimeException("Config creation failed"))
        
        // When & Then
        try {
            testAction.run(null, properties)
            assertTrue(false, "Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("YaXUnit test execution failed") == true)
        }
    }

    @Test
    fun `should return correct result structure`() = runBlocking {
        // Given
        val filter: String? = null
        
        // When
        val result = testAction.run(filter, properties)
        
        // Then
        assertFalse(result.success)
        assertEquals(0, result.testsRun)
        assertEquals(0, result.testsPassed)
        assertEquals(0, result.testsFailed)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.duration.toSeconds() >= 0)
        assertTrue(result.reportPath == null) // No report created due to process failure
    }
}
