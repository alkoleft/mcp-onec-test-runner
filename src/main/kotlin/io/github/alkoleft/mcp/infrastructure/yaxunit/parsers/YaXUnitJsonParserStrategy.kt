package io.github.alkoleft.mcp.infrastructure.yaxunit.parsers

import io.github.alkoleft.mcp.core.modules.ReportFormat
import io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParserStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream

private val logger = KotlinLogging.logger { }

/**
 * Специализированная стратегия для парсинга YaXUnit JSON отчетов
 * Использует JsonReportParserStrategy для обработки
 */
class YaXUnitJsonParserStrategy : ReportParserStrategy {

    private val jsonParser = JsonReportParserStrategy()

    override suspend fun parse(input: InputStream) = jsonParser.parse(input)

    override fun canHandle(format: ReportFormat): Boolean {
        return format == ReportFormat.YAXUNIT_JSON
    }

    override fun getSupportedFormats(): Set<ReportFormat> {
        return setOf(ReportFormat.YAXUNIT_JSON)
    }

    override suspend fun detectFormat(input: InputStream): ReportFormat {
        val content = String(input.readAllBytes())
        return if (content.trim().startsWith("{") && content.contains("testResults")) {
            ReportFormat.YAXUNIT_JSON
        } else {
            throw IllegalArgumentException("Content is not a valid YaXUnit JSON format")
        }
    }
}
