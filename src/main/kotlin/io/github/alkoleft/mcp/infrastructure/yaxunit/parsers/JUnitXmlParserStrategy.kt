package io.github.alkoleft.mcp.infrastructure.yaxunit.parsers

import io.github.alkoleft.mcp.core.modules.GenericTestCase
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.GenericTestSuite
import io.github.alkoleft.mcp.core.modules.ReportFormat
import io.github.alkoleft.mcp.core.modules.TestMetadata
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.alkoleft.mcp.core.modules.TestSummary
import io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParserStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory

private val logger = KotlinLogging.logger { }

/**
 * Специализированная стратегия для парсинга jUnit XML отчетов
 * Поддерживает множественные test suites и детальную обработку ошибок
 */
class JUnitXmlParserStrategy : ReportParserStrategy {

    override suspend fun parse(input: InputStream): GenericTestReport = withContext(Dispatchers.IO) {
        logger.debug { "Parsing jUnit XML report" }

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(input)

        val testSuites = mutableListOf<GenericTestSuite>()
        var totalTests = 0
        var totalPassed = 0
        var totalFailed = 0
        var totalSkipped = 0
        var totalErrors = 0

        // Парсим testsuite элементы
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

        val metadata = TestMetadata(
            environment = mapOf("format" to "junit_xml"),
            configuration = emptyMap(),
            tags = emptySet()
        )

        logger.info { "Parsed jUnit XML report: $totalTests tests, $totalPassed passed, $totalFailed failed" }

        GenericTestReport(
            metadata = metadata,
            summary = summary,
            testSuites = testSuites,
            timestamp = Instant.now(),
            duration = Duration.ZERO // TODO: Извлечь из XML
        )
    }

    override fun canHandle(format: ReportFormat): Boolean {
        return format == ReportFormat.JUNIT_XML
    }

    override fun getSupportedFormats(): Set<ReportFormat> {
        return setOf(ReportFormat.JUNIT_XML)
    }

    override suspend fun detectFormat(input: InputStream): ReportFormat = withContext(Dispatchers.IO) {
        val content = String(input.readAllBytes())
        if (content.trim().startsWith("<?xml") && content.contains("testsuite")) {
            ReportFormat.JUNIT_XML
        } else {
            throw IllegalArgumentException("Content is not a valid jUnit XML format")
        }
    }

    /**
     * Парсит отдельный testsuite элемент
     */
    private fun parseTestSuite(element: Element): GenericTestSuite {
        val name = element.getAttribute("name")
        val tests = element.getAttribute("tests").toIntOrNull() ?: 0
        val failures = element.getAttribute("failures").toIntOrNull() ?: 0
        val errors = element.getAttribute("errors").toIntOrNull() ?: 0
        val skipped = element.getAttribute("skipped").toIntOrNull() ?: 0
        val time = element.getAttribute("time").toDoubleOrNull() ?: 0.0

        logger.debug { "Parsing test suite: $name ($tests tests, $failures failures, $errors errors)" }

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

    /**
     * Парсит отдельный testcase элемент
     */
    private fun parseTestCase(element: Element): GenericTestCase {
        val name = element.getAttribute("name")
        val className = element.getAttribute("classname").takeIf { it.isNotBlank() }
        val time = element.getAttribute("time").toDoubleOrNull() ?: 0.0

        // Определяем статус теста
        val status = when {
            element.getElementsByTagName("failure").length > 0 -> TestStatus.FAILED
            element.getElementsByTagName("error").length > 0 -> TestStatus.ERROR
            element.getElementsByTagName("skipped").length > 0 -> TestStatus.SKIPPED
            else -> TestStatus.PASSED
        }

        // Извлекаем сообщение об ошибке
        val errorMessage = when (status) {
            TestStatus.FAILED -> {
                val failureElement = element.getElementsByTagName("failure").item(0) as? Element
                failureElement?.getAttribute("message") ?: failureElement?.textContent
            }

            TestStatus.ERROR -> {
                val errorElement = element.getElementsByTagName("error").item(0) as? Element
                errorElement?.getAttribute("message") ?: errorElement?.textContent
            }

            else -> null
        }

        // Извлекаем stack trace
        val stackTrace = when (status) {
            TestStatus.FAILED -> {
                val failureElement = element.getElementsByTagName("failure").item(0) as? Element
                failureElement?.textContent
            }

            TestStatus.ERROR -> {
                val errorElement = element.getElementsByTagName("error").item(0) as? Element
                errorElement?.textContent
            }

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
