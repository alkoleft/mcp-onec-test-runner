package io.github.alkoleft.mcp.infrastructure.strategy.parsers

import io.github.alkoleft.mcp.core.modules.ReportFormat
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.alkoleft.mcp.infrastructure.yaxunit.parsers.JUnitXmlParserStrategy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JUnitXmlParserStrategyTest {

    private val parser = JUnitXmlParserStrategy()

    @Test
    fun `should parse valid jUnit XML report`() = runBlocking {
        // Given
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuites>
                <testsuite name="TestSuite1" tests="3" failures="1" errors="0" skipped="0" time="1.5">
                    <testcase name="Test1" classname="TestClass" time="0.5">
                    </testcase>
                    <testcase name="Test2" classname="TestClass" time="0.5">
                        <failure message="Test failed">Failure details</failure>
                    </testcase>
                    <testcase name="Test3" classname="TestClass" time="0.5">
                    </testcase>
                </testsuite>
            </testsuites>
        """.trimIndent()

        val input = ByteArrayInputStream(xmlContent.toByteArray())

        // When
        val result = parser.parse(input)

        // Then
        assertNotNull(result)
        assertEquals(3, result.summary.totalTests)
        assertEquals(2, result.summary.passed)
        assertEquals(1, result.summary.failed)
        assertEquals(0, result.summary.skipped)
        assertEquals(1, result.testSuites.size)

        val testSuite = result.testSuites.first()
        assertEquals("TestSuite1", testSuite.name)
        assertEquals(3, testSuite.tests)
        assertEquals(2, testSuite.passed)
        assertEquals(1, testSuite.failed)

        val testCases = testSuite.testCases
        assertEquals(3, testCases.size)
        assertEquals("Test1", testCases[0].name)
        assertEquals(TestStatus.PASSED, testCases[0].status)
        assertEquals("Test2", testCases[1].name)
        assertEquals(TestStatus.FAILED, testCases[1].status)
        assertEquals("Test failed", testCases[1].errorMessage)
        assertEquals("Test3", testCases[2].name)
        assertEquals(TestStatus.PASSED, testCases[2].status)
    }

    @Test
    fun `should handle multiple test suites`() = runBlocking {
        // Given
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuites>
                <testsuite name="Suite1" tests="2" failures="0" errors="0" skipped="0" time="1.0">
                    <testcase name="Test1" time="0.5">
                    </testcase>
                    <testcase name="Test2" time="0.5">
                    </testcase>
                </testsuite>
                <testsuite name="Suite2" tests="1" failures="1" errors="0" skipped="0" time="0.5">
                    <testcase name="Test3" time="0.5">
                        <failure message="Failed">Details</failure>
                    </testcase>
                </testsuite>
            </testsuites>
        """.trimIndent()

        val input = ByteArrayInputStream(xmlContent.toByteArray())

        // When
        val result = parser.parse(input)

        // Then
        assertNotNull(result)
        assertEquals(3, result.summary.totalTests)
        assertEquals(2, result.summary.passed)
        assertEquals(1, result.summary.failed)
        assertEquals(2, result.testSuites.size)
    }

    @Test
    fun `should handle skipped tests`() = runBlocking {
        // Given
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuites>
                <testsuite name="TestSuite" tests="2" failures="0" errors="0" skipped="1" time="1.0">
                    <testcase name="Test1" time="0.5">
                    </testcase>
                    <testcase name="Test2" time="0.5">
                        <skipped/>
                    </testcase>
                </testsuite>
            </testsuites>
        """.trimIndent()

        val input = ByteArrayInputStream(xmlContent.toByteArray())

        // When
        val result = parser.parse(input)

        // Then
        assertNotNull(result)
        assertEquals(2, result.summary.totalTests)
        assertEquals(1, result.summary.passed)
        assertEquals(0, result.summary.failed)
        assertEquals(1, result.summary.skipped)

        val testCases = result.testSuites.first().testCases
        assertEquals(TestStatus.PASSED, testCases[0].status)
        assertEquals(TestStatus.SKIPPED, testCases[1].status)
    }

    @Test
    fun `should handle error tests`() = runBlocking {
        // Given
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuites>
                <testsuite name="TestSuite" tests="1" failures="0" errors="1" skipped="0" time="1.0">
                    <testcase name="Test1" time="0.5">
                        <error message="Error occurred">Error details</error>
                    </testcase>
                </testsuite>
            </testsuites>
        """.trimIndent()

        val input = ByteArrayInputStream(xmlContent.toByteArray())

        // When
        val result = parser.parse(input)

        // Then
        assertNotNull(result)
        assertEquals(1, result.summary.totalTests)
        assertEquals(0, result.summary.passed)
        assertEquals(0, result.summary.failed)
        assertEquals(0, result.summary.skipped)
        assertEquals(1, result.summary.errors)

        val testCase = result.testSuites.first().testCases.first()
        assertEquals(TestStatus.ERROR, testCase.status)
        assertEquals("Error occurred", testCase.errorMessage)
    }

    @Test
    fun `should detect jUnit XML format`() = runBlocking {
        // Given
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuites>
                <testsuite name="TestSuite" tests="1" failures="0" errors="0" skipped="0" time="1.0">
                    <testcase name="Test1" time="0.5">
                    </testcase>
                </testsuite>
            </testsuites>
        """.trimIndent()

        val input = ByteArrayInputStream(xmlContent.toByteArray())

        // When
        val format = parser.detectFormat(input)

        // Then
        assertEquals(ReportFormat.JUNIT_XML, format)
    }

    @Test
    fun `should throw exception for invalid format`() = runBlocking {
        // Given
        val invalidContent = "This is not XML"
        val input = ByteArrayInputStream(invalidContent.toByteArray())

        // When & Then
        assertThrows<IllegalArgumentException> {
            runBlocking { parser.detectFormat(input) }
        }
    }

    @Test
    fun `should support jUnit XML format`() {
        // When
        val supportedFormats = parser.getSupportedFormats()

        // Then
        assertEquals(setOf(ReportFormat.JUNIT_XML), supportedFormats)
    }

    @Test
    fun `should handle jUnit XML format`() {
        // When & Then
        assert(parser.canHandle(ReportFormat.JUNIT_XML))
        assert(!parser.canHandle(ReportFormat.JSON))
    }
}
