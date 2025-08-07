package io.github.alkoleft.mcp.infrastructure.strategy.parsers

import io.github.alkoleft.mcp.core.modules.GenericTestCase
import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.GenericTestSuite
import io.github.alkoleft.mcp.core.modules.ReportFormat
import io.github.alkoleft.mcp.core.modules.TestMetadata
import io.github.alkoleft.mcp.core.modules.TestStatus
import io.github.alkoleft.mcp.core.modules.TestSummary
import io.github.alkoleft.mcp.core.modules.strategy.ReportParserStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Специализированная стратегия для парсинга простых текстовых отчетов
 * Поддерживает базовые форматы текстовых отчетов
 */
class PlainTextParserStrategy : ReportParserStrategy {

    override suspend fun parse(input: InputStream): GenericTestReport = withContext(Dispatchers.IO) {
        logger.debug { "Parsing plain text report" }

        val content = String(input.readAllBytes())
        val lines = content.lines()

        val testCases = mutableListOf<GenericTestCase>()
        var totalTests = 0
        var totalPassed = 0
        var totalFailed = 0
        var totalSkipped = 0

        for (line in lines) {
            when {
                line.contains("PASSED") || line.contains("ПРОЙДЕН") -> {
                    totalPassed++
                    testCases.add(
                        GenericTestCase(
                            name = extractTestName(line),
                            className = null,
                            status = TestStatus.PASSED,
                            duration = Duration.ZERO,
                            errorMessage = null
                        )
                    )
                }

                line.contains("FAILED") || line.contains("ПРОВАЛЕН") -> {
                    totalFailed++
                    testCases.add(
                        GenericTestCase(
                            name = extractTestName(line),
                            className = null,
                            status = TestStatus.FAILED,
                            duration = Duration.ZERO,
                            errorMessage = extractErrorMessage(line)
                        )
                    )
                }

                line.contains("SKIPPED") || line.contains("ПРОПУЩЕН") -> {
                    totalSkipped++
                    testCases.add(
                        GenericTestCase(
                            name = extractTestName(line),
                            className = null,
                            status = TestStatus.SKIPPED,
                            duration = Duration.ZERO,
                            errorMessage = null
                        )
                    )
                }

                line.contains("ERROR") || line.contains("ОШИБКА") -> {
                    totalFailed++
                    testCases.add(
                        GenericTestCase(
                            name = extractTestName(line),
                            className = null,
                            status = TestStatus.ERROR,
                            duration = Duration.ZERO,
                            errorMessage = extractErrorMessage(line)
                        )
                    )
                }
            }
        }

        totalTests = totalPassed + totalFailed + totalSkipped

        val testSuite = GenericTestSuite(
            name = "Plain Text Tests",
            tests = totalTests,
            passed = totalPassed,
            failed = totalFailed,
            skipped = totalSkipped,
            duration = Duration.ZERO,
            testCases = testCases
        )

        val summary = TestSummary(
            totalTests = totalTests,
            passed = totalPassed,
            failed = totalFailed,
            skipped = totalSkipped,
            errors = 0
        )

        val metadata = TestMetadata(
            environment = mapOf("format" to "plain_text"),
            configuration = emptyMap(),
            tags = emptySet()
        )

        logger.info { "Parsed plain text report: $totalTests tests, $totalPassed passed, $totalFailed failed" }

        GenericTestReport(
            metadata = metadata,
            summary = summary,
            testSuites = listOf(testSuite),
            timestamp = Instant.now(),
            duration = Duration.ZERO
        )
    }

    override fun canHandle(format: ReportFormat): Boolean {
        return format == ReportFormat.PLAIN_TEXT
    }

    override fun getSupportedFormats(): Set<ReportFormat> {
        return setOf(ReportFormat.PLAIN_TEXT)
    }

    override suspend fun detectFormat(input: InputStream): ReportFormat = withContext(Dispatchers.IO) {
        val content = String(input.readAllBytes())
        if (!content.trim().startsWith("<?xml") && !content.trim().startsWith("{")) {
            ReportFormat.PLAIN_TEXT
        } else {
            throw IllegalArgumentException("Content is not a valid plain text format")
        }
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
