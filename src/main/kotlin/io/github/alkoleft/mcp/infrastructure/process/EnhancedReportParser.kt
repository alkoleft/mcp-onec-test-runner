package io.github.alkoleft.mcp.infrastructure.process

import io.github.alkoleft.mcp.core.modules.GenericTestCase
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.GenericTestSuite
import io.github.alkoleft.mcp.core.modules.ReportFormat
import io.github.alkoleft.mcp.core.modules.ReportParser
import io.github.alkoleft.mcp.core.modules.TestMetadata
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.alkoleft.mcp.core.modules.TestSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Minimal JUnit-only report parser
 */
class EnhancedReportParser : ReportParser {
    override suspend fun parseReport(input: InputStream, format: ReportFormat): GenericTestReport =
        withContext(Dispatchers.IO) {
            require(format == ReportFormat.JUNIT_XML) { "Only JUNIT_XML format is supported" }
            parseJUnitXml(input)
    }

    override suspend fun detectFormat(input: InputStream): ReportFormat = withContext(Dispatchers.IO) {
        val bytes = input.readAllBytes()
        val content = String(bytes)
        if (content.trim()
                .startsWith("<?xml") && content.contains("testsuite")
        ) ReportFormat.JUNIT_XML else ReportFormat.JUNIT_XML
    }

    override fun getSupportedFormats(): Set<ReportFormat> = setOf(ReportFormat.JUNIT_XML)

    private fun parseJUnitXml(input: InputStream): GenericTestReport {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(input)

        val testSuites = mutableListOf<GenericTestSuite>()
        var totalTests = 0
        var totalPassed = 0
        var totalFailed = 0
        var totalSkipped = 0
        var totalErrors = 0

        val testSuiteNodes = document.getElementsByTagName("testsuite")
        for (i in 0 until testSuiteNodes.length) {
            val testSuiteElement = testSuiteNodes.item(i) as Element
            val testSuite = parseTestSuite(testSuiteElement)
            testSuites.add(testSuite)
            totalTests += testSuite.tests
            totalPassed += testSuite.passed
            totalFailed += testSuite.failed
            totalSkipped += testSuite.skipped
            totalErrors += testSuite.errors
        }

        val summary = TestSummary(
            totalTests = totalTests,
            passed = totalPassed,
            failed = totalFailed,
            skipped = totalSkipped,
            errors = totalErrors
        )

        val metadata = TestMetadata(environment = mapOf("format" to "junit_xml"))

        return GenericTestReport(
            metadata = metadata,
            summary = summary,
            testSuites = testSuites,
            timestamp = Instant.now(),
            duration = Duration.ZERO
        )
    }

    private fun parseTestSuite(element: Element): GenericTestSuite {
        val name = element.getAttribute("name")
        val tests = element.getAttribute("tests").toIntOrNull() ?: 0
        val failures = element.getAttribute("failures").toIntOrNull() ?: 0
        val errors = element.getAttribute("errors").toIntOrNull() ?: 0
        val skipped = element.getAttribute("skipped").toIntOrNull() ?: 0
        val time = element.getAttribute("time").toDoubleOrNull() ?: 0.0

        val testCases = mutableListOf<GenericTestCase>()
        val testCaseNodes = element.getElementsByTagName("testcase")
        for (i in 0 until testCaseNodes.length) {
            val testCaseElement = testCaseNodes.item(i) as Element
            val testCase = parseTestCase(testCaseElement)
            testCases.add(testCase)
        }

        val passed = tests - failures - errors - skipped

        return GenericTestSuite(
            name = name,
            tests = tests,
            passed = passed,
            failed = failures,
            skipped = skipped,
            errors = errors,
            duration = Duration.ofMillis((time * 1000).toLong()),
            testCases = testCases
        )
    }

    private fun parseTestCase(element: Element): GenericTestCase {
        val name = element.getAttribute("name")
        val className = element.getAttribute("classname").takeIf { it.isNotBlank() }
        val time = element.getAttribute("time").toDoubleOrNull() ?: 0.0

        val status = when {
            element.getElementsByTagName("failure").length > 0 -> TestStatus.FAILED
            element.getElementsByTagName("error").length > 0 -> TestStatus.ERROR
            element.getElementsByTagName("skipped").length > 0 -> TestStatus.SKIPPED
            else -> TestStatus.PASSED
        }

        val errorMessage = when (status) {
            TestStatus.FAILED -> (element.getElementsByTagName("failure").item(0) as? Element)?.getAttribute("message")
                ?: (element.getElementsByTagName("failure").item(0) as? Element)?.textContent

            TestStatus.ERROR -> (element.getElementsByTagName("error").item(0) as? Element)?.getAttribute("message")
                ?: (element.getElementsByTagName("error").item(0) as? Element)?.textContent
            else -> null
        }

        val stackTrace = when (status) {
            TestStatus.FAILED -> (element.getElementsByTagName("failure").item(0) as? Element)?.textContent
            TestStatus.ERROR -> (element.getElementsByTagName("error").item(0) as? Element)?.textContent
            else -> null
        }

        return GenericTestCase(
            name = name,
            className = className,
            status = status,
            duration = Duration.ofMillis((time * 1000).toLong()),
            errorMessage = errorMessage,
            stackTrace = stackTrace,
            systemOut = null,
            systemErr = null
        )
    }
}
