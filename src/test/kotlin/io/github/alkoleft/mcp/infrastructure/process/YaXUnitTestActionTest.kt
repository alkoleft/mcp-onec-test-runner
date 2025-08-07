package io.github.alkoleft.mcp.infrastructure.process

import io.github.alkoleft.mcp.application.actions.test.YaXUnitTestAction
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assert

class YaXUnitTestActionTest {

    @Mock
    private lateinit var platformUtilityDsl: PlatformUtilityDsl

    @Mock
    private lateinit var utilLocator: CrossPlatformUtilLocator

    private lateinit var configWriter: JsonYaXUnitConfigWriter
    private lateinit var reportParser: EnhancedReportParser
    private lateinit var testAction: YaXUnitTestAction

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        configWriter = JsonYaXUnitConfigWriter()
        reportParser = EnhancedReportParser()
        testAction = YaXUnitTestAction(platformUtilityDsl, utilLocator, configWriter, reportParser)
    }

    @Test
    fun `test YaXUnit test action creation`() {
        assertNotNull(testAction)
    }

    @Test
    fun `test config writer creates valid JSON config`() = runBlocking {
        val properties = createTestProperties()
        val request = io.github.alkoleft.mcp.core.modules.RunAllTestsRequest(
            projectPath = tempDir,
            testsPath = tempDir.resolve("tests"),
            ibConnection = "File=\"test.ib\";",
            platformVersion = "8.3.18"
        )

        val configPath = configWriter.createTempConfig(request)
        
        assertNotNull(configPath)
        assert(configPath.toFile().exists())
        assert(configPath.toFile().length() > 0)
    }

    @Test
    fun `test report parser detects format correctly`() = runBlocking {
        // Test JUnit XML format detection
        val junitXml = """<?xml version="1.0" encoding="UTF-8"?>
            <testsuites>
                <testsuite name="TestSuite" tests="1" failures="0" errors="0" time="0.1">
                    <testcase name="testMethod" classname="TestClass" time="0.1"/>
                </testsuite>
            </testsuites>""".trimIndent().byteInputStream()

        val format = reportParser.detectFormat(junitXml)
        assertEquals(io.github.alkoleft.mcp.core.modules.ReportFormat.JUNIT_XML, format)
    }

    @Test
    fun `test report parser parses JUnit XML`() = runBlocking {
        val junitXml = """<?xml version="1.0" encoding="UTF-8"?>
            <testsuites>
                <testsuite name="TestSuite" tests="2" failures="1" errors="0" time="0.2">
                    <testcase name="testPass" classname="TestClass" time="0.1"/>
                    <testcase name="testFail" classname="TestClass" time="0.1">
                        <failure message="Test failed"/>
                    </testcase>
                </testsuite>
            </testsuites>""".trimIndent().byteInputStream()

        val report = reportParser.parseReport(junitXml, io.github.alkoleft.mcp.core.modules.ReportFormat.JUNIT_XML)
        
        assertEquals(2, report.summary.totalTests)
        assertEquals(1, report.summary.passed)
        assertEquals(1, report.summary.failed)
        assertEquals(1, report.testSuites.size)
        assertEquals("TestSuite", report.testSuites[0].name)
    }

    @Test
    fun `test report parser parses JSON format`() = runBlocking {
        val jsonReport = """{
            "testResults": [
                {
                    "moduleName": "TestModule",
                    "tests": [
                        {
                            "name": "testMethod",
                            "result": "PASSED"
                        }
                    ]
                }
            ]
        }""".trimIndent().byteInputStream()

        val report = reportParser.parseReport(jsonReport, io.github.alkoleft.mcp.core.modules.ReportFormat.YAXUNIT_JSON)
        
        assertEquals(1, report.summary.totalTests)
        assertEquals(1, report.summary.passed)
        assertEquals(0, report.summary.failed)
        assertEquals(1, report.testSuites.size)
        assertEquals("TestModule", report.testSuites[0].name)
    }

    @Test
    fun `test report parser parses plain text`() = runBlocking {
        val plainText = """
            Test1 PASSED
            Test2 FAILED: Assertion failed
            Test3 PASSED
        """.trimIndent().byteInputStream()

        val report = reportParser.parseReport(plainText, io.github.alkoleft.mcp.core.modules.ReportFormat.PLAIN_TEXT)
        
        assertEquals(3, report.summary.totalTests)
        assertEquals(2, report.summary.passed)
        assertEquals(1, report.summary.failed)
        assertEquals(1, report.testSuites.size)
        assertEquals("Plain Text Tests", report.testSuites[0].name)
    }

    private fun createTestProperties(): ApplicationProperties {
        val sourceSet = SourceSet(
            listOf(
                SourceSetItem(
                    name = "configuration",
                    path = "src",
                    type = SourceSetType.CONFIGURATION,
                    purpose = setOf(SourceSetPurpose.MAIN)
                ),
                SourceSetItem(
                    name = "tests",
                    path = "tests",
                    type = SourceSetType.CONFIGURATION,
                    purpose = setOf(SourceSetPurpose.TESTS)
                )
            )
        )

        val connection = ConnectionProperties(
            connectionString = "File=\"test.ib\";",
            user = "Admin",
            password = null
        )

        val tools = ToolsProperties(
            builder = io.github.alkoleft.mcp.configuration.properties.BuilderType.DESIGNER
        )

        return ApplicationProperties(
            basePath = tempDir,
            sourceSet = sourceSet,
            connection = connection,
            platformVersion = "8.3.18",
            tools = tools
        )
    }
} 