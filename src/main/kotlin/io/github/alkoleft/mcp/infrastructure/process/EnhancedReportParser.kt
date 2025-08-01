package io.github.alkoleft.mcp.infrastructure.process

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.alkoleft.mcp.core.modules.GenericTestCase
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.GenericTestSuite
import io.github.alkoleft.mcp.core.modules.ReportFormat
import io.github.alkoleft.mcp.core.modules.ReportParser
import io.github.alkoleft.mcp.core.modules.TestExecutionError
import io.github.alkoleft.mcp.core.modules.TestMetadata
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.alkoleft.mcp.core.modules.TestSummary
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.BufferedInputStream
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Enhanced multi-format test report parser implementing Strategy Pattern with streaming support.
 * Supports JUnit XML, JSON, and YAXUnit-specific formats with automatic format detection.
 */
private val logger = KotlinLogging.logger {  }

@Component
class EnhancedReportParser : ReportParser {
    private val formatDetector = FormatDetector()
    private val parserRegistry = ParserRegistry()

    init {
        // Register built-in parsers
        parserRegistry.register(ReportFormat.JUNIT_XML, JUnitXmlParser())
        parserRegistry.register(ReportFormat.JSON, JsonReportParser())
        parserRegistry.register(ReportFormat.YAXUNIT_JSON, YaXUnitJsonParser())
    }

    override suspend fun parseReport(
        input: InputStream,
        format: ReportFormat,
    ): GenericTestReport =
        withContext(Dispatchers.IO) {
            val parser =
                parserRegistry.getParser(format)
                    ?: throw TestExecutionError.ReportParsingFailed("No parser available for format: $format")

            try {
                logger.debug { "Parsing report with format: $format" }
                val rawReport = parser.parse(input)
                val normalizedReport = normalizeToGeneric(rawReport)

                logger.info { "Successfully parsed report: ${normalizedReport.summary.totalTests} tests, ${normalizedReport.testSuites.size} suites" }
                normalizedReport
            } catch (e: Exception) {
                logger.error(e) { "Failed to parse report with format $format" }
                throw TestExecutionError.ReportParsingFailed("Report parsing failed: ${e.message}")
            }
        }

    override suspend fun detectFormat(input: InputStream): ReportFormat =
        withContext(Dispatchers.IO) {
            val bufferedInput = input as? BufferedInputStream ?: BufferedInputStream(input)

            try {
                formatDetector.detectFormat(bufferedInput)
            } catch (e: Exception) {
                logger.error(e) { "Failed to detect report format" }
                throw TestExecutionError.ReportParsingFailed("Format detection failed: ${e.message}")
            }
        }

    override fun getSupportedFormats(): Set<ReportFormat> = parserRegistry.getSupportedFormats()

    /**
     * Normalizes raw test report to generic format
     */
    private fun normalizeToGeneric(rawReport: RawTestReport): GenericTestReport =
        GenericTestReport(
            metadata =
                TestMetadata(
                    environment = rawReport.environment ?: emptyMap(),
                    configuration = rawReport.configuration ?: emptyMap(),
                    tags = rawReport.tags ?: emptySet(),
                ),
            summary = calculateSummary(rawReport),
            testSuites = rawReport.testSuites.map { normalizeTestSuite(it) },
            timestamp = rawReport.timestamp ?: Instant.now(),
            duration = rawReport.duration ?: Duration.ZERO,
        )

    private fun calculateSummary(rawReport: RawTestReport): TestSummary {
        val allTests = rawReport.testSuites.flatMap { it.testCases }

        return TestSummary(
            totalTests = allTests.size,
            passed = allTests.count { it.status == TestStatus.PASSED },
            failed = allTests.count { it.status == TestStatus.FAILED },
            skipped = allTests.count { it.status == TestStatus.SKIPPED },
            errors = allTests.count { it.status == TestStatus.ERROR },
        )
    }

    private fun normalizeTestSuite(rawSuite: RawTestSuite): GenericTestSuite =
        GenericTestSuite(
            name = rawSuite.name,
            tests = rawSuite.testCases.size,
            passed = rawSuite.testCases.count { it.status == TestStatus.PASSED },
            failed = rawSuite.testCases.count { it.status == TestStatus.FAILED },
            skipped = rawSuite.testCases.count { it.status == TestStatus.SKIPPED },
            duration = rawSuite.duration ?: Duration.ZERO,
            testCases = rawSuite.testCases.map { normalizeTestCase(it) },
        )

    private fun normalizeTestCase(rawCase: RawTestCase): GenericTestCase =
        GenericTestCase(
            name = rawCase.name,
            className = rawCase.className,
            status = rawCase.status,
            duration = rawCase.duration ?: Duration.ZERO,
            errorMessage = rawCase.errorMessage,
            stackTrace = rawCase.stackTrace,
            systemOut = rawCase.systemOut,
            systemErr = rawCase.systemErr,
        )
}

/**
 * Format detection with confidence scoring
 */
class FormatDetector {
    private val logger = KotlinLogging.logger {  }

    fun detectFormat(input: BufferedInputStream): ReportFormat {
        input.mark(8192) // Mark for reset

        try {
            val preview = ByteArray(4096)
            val bytesRead = input.read(preview)
            input.reset() // Reset for actual parsing

            if (bytesRead <= 0) {
                throw RuntimeException("Empty input stream")
            }

            val content = String(preview, 0, bytesRead, Charsets.UTF_8)

            val detectionResults =
                listOf(
                    detectJUnitXml(content),
                    detectJson(content),
                    detectYaXUnitJson(content),
                ).sortedByDescending { it.confidence }

            val bestMatch =
                detectionResults.firstOrNull { it.confidence > 0.7 }
                    ?: throw RuntimeException("Cannot determine report format with sufficient confidence")

            logger.debug { "Detected format: ${bestMatch.format} (confidence: ${bestMatch.confidence})" }
            return bestMatch.format
        } catch (e: Exception) {
            logger.error(e) { "Format detection failed" }
            throw e
        }
    }

    private fun detectJUnitXml(content: String): DetectionResult {
        var confidence = 0.0
        val indicators = mutableListOf<String>()

        if (content.contains("<?xml")) {
            confidence += 0.3
            indicators.add("XML_DECLARATION")
        }

        if (content.contains("<testsuite") || content.contains("<testsuites")) {
            confidence += 0.5
            indicators.add("JUNIT_ELEMENTS")
        }

        if (content.contains("tests=") && content.contains("failures=")) {
            confidence += 0.2
            indicators.add("JUNIT_ATTRIBUTES")
        }

        return DetectionResult(ReportFormat.JUNIT_XML, confidence, indicators)
    }

    private fun detectJson(content: String): DetectionResult {
        var confidence = 0.0
        val indicators = mutableListOf<String>()

        val trimmed = content.trim()
        if (trimmed.startsWith("{") && trimmed.contains("}")) {
            confidence += 0.4
            indicators.add("JSON_STRUCTURE")
        }

        if (content.contains("\"tests\"") || content.contains("\"testSuites\"")) {
            confidence += 0.3
            indicators.add("TEST_FIELDS")
        }

        if (content.contains("\"status\"") && content.contains("\"duration\"")) {
            confidence += 0.2
            indicators.add("COMMON_FIELDS")
        }

        return DetectionResult(ReportFormat.JSON, confidence, indicators)
    }

    private fun detectYaXUnitJson(content: String): DetectionResult {
        var confidence = 0.0
        val indicators = mutableListOf<String>()

        val trimmed = content.trim()
        if (trimmed.startsWith("{") && trimmed.contains("}")) {
            confidence += 0.2
            indicators.add("JSON_STRUCTURE")
        }

        if (content.contains("YAXUnit") || content.contains("yaxunit")) {
            confidence += 0.6
            indicators.add("YAXUNIT_MARKER")
        }

        if (content.contains("\"1C:Enterprise\"") || content.contains("\"1cv8\"")) {
            confidence += 0.2
            indicators.add("1C_MARKERS")
        }

        return DetectionResult(ReportFormat.YAXUNIT_JSON, confidence, indicators)
    }

    data class DetectionResult(
        val format: ReportFormat,
        val confidence: Double,
        val indicators: List<String>,
    )
}

/**
 * Registry for managing parsers
 */
class ParserRegistry {
    private val parsers = mutableMapOf<ReportFormat, ReportFormatParser>()

    fun register(
        format: ReportFormat,
        parser: ReportFormatParser,
    ) {
        parsers[format] = parser
    }

    fun getParser(format: ReportFormat): ReportFormatParser? = parsers[format]

    fun getSupportedFormats(): Set<ReportFormat> = parsers.keys
}

/**
 * Base interface for format-specific parsers
 */
interface ReportFormatParser {
    suspend fun parse(input: InputStream): RawTestReport
}

/**
 * JUnit XML parser implementation
 */
class JUnitXmlParser : ReportFormatParser {
    private val logger = KotlinLogging.logger {  }

    override suspend fun parse(input: InputStream): RawTestReport =
        withContext(Dispatchers.IO) {
            try {
                val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val document = documentBuilder.parse(input)
                parseJUnitDocument(document)
            } catch (e: SAXException) {
                logger.error(e) { "Failed to parse JUnit XML" }
                throw TestExecutionError.ReportParsingFailed("Invalid XML format: ${e.message}")
            }
        }

    private fun parseJUnitDocument(document: Document): RawTestReport {
        val root = document.documentElement

        val testSuites =
            when (root.nodeName) {
                "testsuites" -> parseTestSuites(root)
                "testsuite" -> listOf(parseTestSuite(root))
                else -> throw TestExecutionError.ReportParsingFailed("Unknown root element: ${root.nodeName}")
            }

        return RawTestReport(
            testSuites = testSuites,
            timestamp = Instant.now(),
            duration =
                testSuites.fold(Duration.ZERO) { acc, suite ->
                    acc.plus(suite.duration ?: Duration.ZERO)
                },
        )
    }

    private fun parseTestSuites(element: Element): List<RawTestSuite> {
        val suiteNodes = element.getElementsByTagName("testsuite")
        return (0 until suiteNodes.length).map { i ->
            parseTestSuite(suiteNodes.item(i) as Element)
        }
    }

    private fun parseTestSuite(element: Element): RawTestSuite {
        val name = element.getAttribute("name") ?: "Unknown Suite"
        val time = element.getAttribute("time").toDoubleOrNull() ?: 0.0

        val testCaseNodes = element.getElementsByTagName("testcase")
        val testCases =
            (0 until testCaseNodes.length).map { i ->
                parseTestCase(testCaseNodes.item(i) as Element)
            }

        return RawTestSuite(
            name = name,
            testCases = testCases,
            duration = Duration.ofMillis((time * 1000).toLong()),
        )
    }

    private fun parseTestCase(element: Element): RawTestCase {
        val name = element.getAttribute("name") ?: "Unknown Test"
        val className = element.getAttribute("classname")
        val time = element.getAttribute("time").toDoubleOrNull() ?: 0.0

        // Determine status based on child elements
        val status =
            when {
                element.getElementsByTagName("failure").length > 0 -> TestStatus.FAILED
                element.getElementsByTagName("error").length > 0 -> TestStatus.ERROR
                element.getElementsByTagName("skipped").length > 0 -> TestStatus.SKIPPED
                else -> TestStatus.PASSED
            }

        // Extract error message and stack trace
        val failureNode = element.getElementsByTagName("failure").item(0)
        val errorNode = element.getElementsByTagName("error").item(0)

        val errorMessage =
            failureNode?.attributes?.getNamedItem("message")?.nodeValue
                ?: errorNode?.attributes?.getNamedItem("message")?.nodeValue

        val stackTrace = failureNode?.textContent ?: errorNode?.textContent

        return RawTestCase(
            name = name,
            className = className,
            status = status,
            duration = Duration.ofMillis((time * 1000).toLong()),
            errorMessage = errorMessage,
            stackTrace = stackTrace,
        )
    }
}

/**
 * Generic JSON parser implementation
 */
class JsonReportParser : ReportFormatParser {
    private val objectMapper =
        ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            registerModule(KotlinModule.Builder().build())
        }

    override suspend fun parse(input: InputStream): RawTestReport =
        withContext(Dispatchers.IO) {
            try {
                objectMapper.readValue(input, RawTestReport::class.java)
            } catch (e: Exception) {
                throw TestExecutionError.ReportParsingFailed("Failed to parse JSON report: ${e.message}")
            }
        }
}

/**
 * YAXUnit-specific JSON parser implementation
 */
class YaXUnitJsonParser : ReportFormatParser {
    private val objectMapper =
        ObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            registerModule(KotlinModule.Builder().build())
        }

    override suspend fun parse(input: InputStream): RawTestReport =
        withContext(Dispatchers.IO) {
            try {
                // YAXUnit has a specific JSON structure - adapt as needed
                val jsonNode = objectMapper.readTree(input)

                // Extract test suites from YAXUnit format
                val testSuites = mutableListOf<RawTestSuite>()

                // YAXUnit typically has a "tests" array or similar structure
                val testsNode = jsonNode.get("tests") ?: jsonNode.get("testResults")

                if (testsNode != null && testsNode.isArray) {
                    for (testNode in testsNode) {
                        val testCase =
                            RawTestCase(
                                name = testNode.get("name")?.asText() ?: "Unknown",
                                className = testNode.get("module")?.asText(),
                                status = parseYaXUnitStatus(testNode.get("status")?.asText() ?: "unknown"),
                                duration = Duration.ofMillis(testNode.get("duration")?.asLong() ?: 0),
                                errorMessage = testNode.get("error")?.asText(),
                                stackTrace = testNode.get("stackTrace")?.asText(),
                            )

                        // Group by module/suite
                        val suiteName = testCase.className ?: "Default Suite"
                        testSuites.find { it.name == suiteName }

                        val existingIndex = testSuites.indexOfFirst { it.name == suiteName }

                        if (existingIndex >= 0) {
                            val existingSuite = testSuites[existingIndex]
                            val updatedSuite =
                                existingSuite.copy(
                                    testCases = existingSuite.testCases + testCase,
                                )
                            testSuites[existingIndex] = updatedSuite
                        } else {
                            testSuites.add(RawTestSuite(suiteName, listOf(testCase)))
                        }
                    }
                }

                RawTestReport(
                    testSuites = testSuites,
                    timestamp = Instant.now(),
                    duration =
                        testSuites.fold(Duration.ZERO) { acc, suite ->
                            acc.plus(suite.duration ?: Duration.ZERO)
                        },
                )
            } catch (e: Exception) {
                throw TestExecutionError.ReportParsingFailed("Failed to parse YAXUnit JSON report: ${e.message}")
            }
        }

    private fun parseYaXUnitStatus(status: String): TestStatus =
        when (status.lowercase()) {
            "passed", "success", "ok" -> TestStatus.PASSED
            "failed", "failure", "fail" -> TestStatus.FAILED
            "skipped", "ignored" -> TestStatus.SKIPPED
            "error", "exception" -> TestStatus.ERROR
            else -> TestStatus.ERROR
        }
}

// Raw data structures for parsing
data class RawTestReport(
    val testSuites: List<RawTestSuite>,
    val timestamp: Instant? = null,
    val duration: Duration? = null,
    val environment: Map<String, String>? = null,
    val configuration: Map<String, String>? = null,
    val tags: Set<String>? = null,
)

data class RawTestSuite(
    val name: String,
    val testCases: List<RawTestCase>,
    val duration: Duration? = null,
)

data class RawTestCase(
    val name: String,
    val className: String? = null,
    val status: TestStatus,
    val duration: Duration? = null,
    val errorMessage: String? = null,
    val stackTrace: String? = null,
    val systemOut: String? = null,
    val systemErr: String? = null,
)
