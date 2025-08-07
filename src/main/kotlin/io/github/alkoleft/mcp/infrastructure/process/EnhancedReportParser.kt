package io.github.alkoleft.mcp.infrastructure.process

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.alkoleft.mcp.core.modules.GenericTestCase
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.GenericTestSuite
import io.github.alkoleft.mcp.core.modules.ReportFormat
import io.github.alkoleft.mcp.core.modules.ReportParser
import io.github.alkoleft.mcp.core.modules.TestMetadata
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.alkoleft.mcp.core.modules.TestSummary
import io.github.alkoleft.mcp.infrastructure.strategy.ReportParserFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Расширенный парсер отчетов о тестировании
 * Поддерживает различные форматы: jUnit XML, JSON, YaXUnit JSON
 * Интегрирован со стратегиями парсинга
 */
class EnhancedReportParser : ReportParser {
    
    private val objectMapper = ObjectMapper()
    private val parserFactory = ReportParserFactory()
    
    override suspend fun parseReport(
        input: InputStream,
        format: ReportFormat
    ): GenericTestReport = withContext(Dispatchers.IO) {
        val strategy = parserFactory.createStrategy(format)
        strategy.parse(input)
    }
    
    override suspend fun detectFormat(input: InputStream): ReportFormat = withContext(Dispatchers.IO) {
        val content = input.readAllBytes()
        val contentString = String(content)
        
        when {
            contentString.trim().startsWith("<?xml") && contentString.contains("testsuite") -> ReportFormat.JUNIT_XML
            contentString.trim().startsWith("{") && contentString.contains("testResults") -> ReportFormat.YAXUNIT_JSON
            contentString.trim().startsWith("{") -> ReportFormat.JSON
            else -> ReportFormat.PLAIN_TEXT
        }
    }
    
    override fun getSupportedFormats(): Set<ReportFormat> {
        return setOf(
            ReportFormat.JUNIT_XML,
            ReportFormat.JSON,
            ReportFormat.YAXUNIT_JSON,
            ReportFormat.PLAIN_TEXT
        )
    }
    
    /**
     * Парсит jUnit XML отчет
     */
    private suspend fun parseJUnitXml(input: InputStream): GenericTestReport = withContext(Dispatchers.IO) {
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
        }
        
        val summary = TestSummary(
            totalTests = totalTests,
            passed = totalPassed,
            failed = totalFailed,
            skipped = totalSkipped,
            errors = totalErrors
        )
        
        val metadata = TestMetadata(
            environment = mapOf("format" to "junit"),
            configuration = emptyMap(),
            tags = emptySet()
        )
        
        GenericTestReport(
            metadata = metadata,
            summary = summary,
            testSuites = testSuites,
            timestamp = Instant.now(),
            duration = Duration.ZERO // TODO: Извлечь из XML
        )
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
                failureElement?.getAttribute("message")
            }
            TestStatus.ERROR -> {
                val errorElement = element.getElementsByTagName("error").item(0) as? Element
                errorElement?.getAttribute("message")
            }
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
     * Парсит JSON отчет
     */
    private suspend fun parseJsonReport(input: InputStream): GenericTestReport = withContext(Dispatchers.IO) {
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
        
        GenericTestReport(
            metadata = metadata,
            summary = summary,
            testSuites = testSuites,
            timestamp = Instant.now(),
            duration = Duration.ZERO
        )
    }
    
    /**
     * Парсит YaXUnit JSON отчет
     */
    private suspend fun parseYaXUnitJson(input: InputStream): GenericTestReport = withContext(Dispatchers.IO) {
        parseJsonReport(input) // Используем общий JSON парсер
    }
    
    /**
     * Парсит простой текстовый отчет
     */
    private suspend fun parsePlainText(input: InputStream): GenericTestReport = withContext(Dispatchers.IO) {
        val content = String(input.readAllBytes())
        val lines = content.lines()
        
        val testCases = mutableListOf<GenericTestCase>()
        var totalTests = 0
        var totalPassed = 0
        var totalFailed = 0
        
        for (line in lines) {
            when {
                line.contains("PASSED") || line.contains("ПРОЙДЕН") -> {
                    totalPassed++
                    testCases.add(GenericTestCase(
                        name = extractTestName(line),
                        className = null,
                        status = TestStatus.PASSED,
                        duration = Duration.ZERO,
                        errorMessage = null
                    ))
                }
                line.contains("FAILED") || line.contains("ПРОВАЛЕН") -> {
                    totalFailed++
                    testCases.add(GenericTestCase(
                        name = extractTestName(line),
                        className = null,
                        status = TestStatus.FAILED,
                        duration = Duration.ZERO,
                        errorMessage = extractErrorMessage(line)
                    ))
                }
                line.contains("SKIPPED") || line.contains("ПРОПУЩЕН") -> {
                    testCases.add(GenericTestCase(
                        name = extractTestName(line),
                        className = null,
                        status = TestStatus.SKIPPED,
                        duration = Duration.ZERO,
                        errorMessage = null
                    ))
                }
            }
        }
        
        totalTests = totalPassed + totalFailed
        
        val testSuite = GenericTestSuite(
            name = "Plain Text Tests",
            tests = totalTests,
            passed = totalPassed,
            failed = totalFailed,
            skipped = 0,
            duration = Duration.ZERO,
            testCases = testCases
        )
        
        val summary = TestSummary(
            totalTests = totalTests,
            passed = totalPassed,
            failed = totalFailed,
            skipped = 0,
            errors = 0
        )
        
        val metadata = TestMetadata(
            environment = mapOf("format" to "plain_text"),
            configuration = emptyMap(),
            tags = emptySet()
        )
        
        GenericTestReport(
            metadata = metadata,
            summary = summary,
            testSuites = listOf(testSuite),
            timestamp = Instant.now(),
            duration = Duration.ZERO
        )
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
    
    /**
     * Извлекает имя теста из строки
     */
    private fun extractTestName(line: String): String {
        return line.substringAfterLast(" ").substringBefore(":").trim()
    }
    
    /**
     * Извлекает сообщение об ошибке из строки
     */
    private fun extractErrorMessage(line: String): String? {
        return if (line.contains(":")) {
            line.substringAfter(":").trim()
        } else null
    }
}
