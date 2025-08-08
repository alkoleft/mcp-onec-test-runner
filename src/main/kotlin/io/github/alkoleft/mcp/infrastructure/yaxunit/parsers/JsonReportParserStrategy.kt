package io.github.alkoleft.mcp.infrastructure.yaxunit.parsers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.io.InputStream
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Специализированная стратегия для парсинга JSON отчетов
 * Поддерживает различные форматы JSON отчетов
 */
class JsonReportParserStrategy : ReportParserStrategy {

    private val objectMapper = ObjectMapper()

    override suspend fun parse(input: InputStream): GenericTestReport = withContext(Dispatchers.IO) {
        logger.debug { "Parsing JSON report" }

        val rootNode = objectMapper.readTree(input)

        val testSuites = mutableListOf<GenericTestSuite>()
        var totalTests = 0
        var totalPassed = 0
        var totalFailed = 0
        var totalSkipped = 0
        var totalErrors = 0

        // Парсим в зависимости от структуры JSON
        when {
            rootNode.has("testResults") -> {
                // YaXUnit формат
                val testResults = rootNode.get("testResults")
                if (testResults.isArray) {
                    for (testResult in testResults) {
                        val testSuite = parseYaXUnitTestResult(testResult)
                        testSuites.add(testSuite)

                        totalTests += testSuite.tests
                        totalPassed += testSuite.passed
                        totalFailed += testSuite.failed
                        totalSkipped += testSuite.skipped
                    }
                }
            }

            rootNode.has("testsuites") -> {
                // Стандартный формат с testsuites
                val testSuitesNode = rootNode.get("testsuites")
                if (testSuitesNode.isArray) {
                    for (testSuiteNode in testSuitesNode) {
                        val testSuite = parseJsonTestSuite(testSuiteNode)
                        testSuites.add(testSuite)

                        totalTests += testSuite.tests
                        totalPassed += testSuite.passed
                        totalFailed += testSuite.failed
                        totalSkipped += testSuite.skipped
                    }
                }
            }

            else -> {
                // Простой формат с массивом тестов
                if (rootNode.isArray) {
                    val testSuite = parseJsonTestSuite(rootNode)
                    testSuites.add(testSuite)

                    totalTests += testSuite.tests
                    totalPassed += testSuite.passed
                    totalFailed += testSuite.failed
                    totalSkipped += testSuite.skipped
                }
            }
        }

        val summary = TestSummary(
            totalTests = totalTests,
            passed = totalPassed,
            failed = totalFailed,
            skipped = totalSkipped,
            errors = totalErrors
        )

        val metadata = TestMetadata(
            environment = mapOf("format" to "json"),
            configuration = emptyMap(),
            tags = emptySet()
        )

        logger.info { "Parsed JSON report: $totalTests tests, $totalPassed passed, $totalFailed failed" }

        GenericTestReport(
            metadata = metadata,
            summary = summary,
            testSuites = testSuites,
            timestamp = Instant.now(),
            duration = Duration.ZERO
        )
    }

    override fun canHandle(format: ReportFormat): Boolean {
        return format == ReportFormat.JSON
    }

    override fun getSupportedFormats(): Set<ReportFormat> {
        return setOf(ReportFormat.JSON)
    }

    override suspend fun detectFormat(input: InputStream): ReportFormat = withContext(Dispatchers.IO) {
        val content = String(input.readAllBytes())
        if (content.trim().startsWith("{") && !content.contains("testResults")) {
            ReportFormat.JSON
        } else {
            throw IllegalArgumentException("Content is not a valid JSON format")
        }
    }

    /**
     * Парсит JSON testsuite
     */
    private fun parseJsonTestSuite(node: JsonNode): GenericTestSuite {
        val name = node.get("name")?.asText() ?: "Unknown Suite"
        val tests = node.get("tests")?.asInt() ?: 0
        val failures = node.get("failures")?.asInt() ?: 0
        val errors = node.get("errors")?.asInt() ?: 0
        val skipped = node.get("skipped")?.asInt() ?: 0
        val time = node.get("time")?.asDouble() ?: 0.0

        logger.debug { "Parsing JSON test suite: $name ($tests tests, $failures failures, $errors errors)" }

        val testCases = mutableListOf<GenericTestCase>()
        val testCasesNode = node.get("testcases")

        if (testCasesNode?.isArray == true) {
            for (testCaseNode in testCasesNode) {
                val testCase = parseJsonTestCase(testCaseNode)
                testCases.add(testCase)
            }
        }

        val passed = tests - failures - errors - skipped

        return GenericTestSuite(
            name = name,
            tests = tests,
            passed = passed,
            failed = failures,
            skipped = skipped,
            duration = Duration.ofMillis((time * 1000).toLong()),
            testCases = testCases
        )
    }

    /**
     * Парсит JSON testcase
     */
    private fun parseJsonTestCase(node: JsonNode): GenericTestCase {
        val name = node.get("name")?.asText() ?: "Unknown Test"
        val className = node.get("classname")?.asText()
        val time = node.get("time")?.asDouble() ?: 0.0

        val status = when {
            node.has("failure") -> TestStatus.FAILED
            node.has("error") -> TestStatus.ERROR
            node.has("skipped") -> TestStatus.SKIPPED
            else -> TestStatus.PASSED
        }

        val errorMessage = when (status) {
            TestStatus.FAILED -> node.get("failure")?.get("message")?.asText()
            TestStatus.ERROR -> node.get("error")?.get("message")?.asText()
            else -> null
        }

        return GenericTestCase(
            name = name,
            className = className,
            status = status,
            duration = Duration.ofMillis((time * 1000).toLong()),
            errorMessage = errorMessage,
            stackTrace = null,
            systemOut = null,
            systemErr = null
        )
    }

    /**
     * Парсит YaXUnit test result
     */
    private fun parseYaXUnitTestResult(node: JsonNode): GenericTestSuite {
        val name = node.get("moduleName")?.asText() ?: "Unknown Module"
        val testCases = mutableListOf<GenericTestCase>()

        val testsNode = node.get("tests")
        if (testsNode?.isArray == true) {
            for (testNode in testsNode) {
                val testCase = parseYaXUnitTestCase(testNode)
                testCases.add(testCase)
            }
        }

        val tests = testCases.size
        val passed = testCases.count { it.status == TestStatus.PASSED }
        val failed = testCases.count { it.status == TestStatus.FAILED }
        val skipped = testCases.count { it.status == TestStatus.SKIPPED }

        return GenericTestSuite(
            name = name,
            tests = tests,
            passed = passed,
            failed = failed,
            skipped = skipped,
            duration = Duration.ZERO,
            testCases = testCases
        )
    }

    /**
     * Парсит YaXUnit test case
     */
    private fun parseYaXUnitTestCase(node: JsonNode): GenericTestCase {
        val name = node.get("name")?.asText() ?: "Unknown Test"
        val status = when (node.get("result")?.asText()) {
            "PASSED" -> TestStatus.PASSED
            "FAILED" -> TestStatus.FAILED
            "SKIPPED" -> TestStatus.SKIPPED
            else -> TestStatus.ERROR
        }

        val errorMessage = if (status == TestStatus.FAILED) {
            node.get("error")?.asText()
        } else null

        return GenericTestCase(
            name = name,
            className = null,
            status = status,
            duration = Duration.ZERO,
            errorMessage = errorMessage,
            stackTrace = null,
            systemOut = null,
            systemErr = null
        )
    }
}
