package io.github.alkoleft.mcp.infrastructure.strategy

import io.github.alkoleft.mcp.core.modules.ReportFormat
import io.github.alkoleft.mcp.core.modules.strategy.ReportParserStrategy
import io.github.alkoleft.mcp.infrastructure.strategy.parsers.JUnitXmlParserStrategy
import io.github.alkoleft.mcp.infrastructure.strategy.parsers.JsonReportParserStrategy
import io.github.alkoleft.mcp.infrastructure.strategy.parsers.PlainTextParserStrategy
import io.github.alkoleft.mcp.infrastructure.strategy.parsers.YaXUnitJsonParserStrategy
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

/**
 * Фабрика для создания стратегий парсинга отчетов
 * Поддерживает все форматы отчетов через Strategy Pattern
 */
class ReportParserFactory {

    private val strategies = mapOf(
        ReportFormat.JUNIT_XML to JUnitXmlParserStrategy(),
        ReportFormat.JSON to JsonReportParserStrategy(),
        ReportFormat.YAXUNIT_JSON to YaXUnitJsonParserStrategy(),
        ReportFormat.PLAIN_TEXT to PlainTextParserStrategy()
    )

    /**
     * Создает стратегию для указанного формата
     */
    fun createStrategy(format: ReportFormat): ReportParserStrategy {
        val strategy = strategies[format]
        if (strategy == null) {
            logger.error { "No strategy found for format: $format" }
            throw IllegalArgumentException("Unsupported report format: $format")
        }

        logger.debug { "Created parser strategy for format: $format" }
        return strategy
    }

    /**
     * Создает стратегию на основе содержимого отчета
     */
    suspend fun createStrategyFromContent(content: String): ReportParserStrategy {
        val format = detectFormatFromContent(content)
        return createStrategy(format)
    }

    /**
     * Возвращает все поддерживаемые форматы
     */
    fun getSupportedFormats(): Set<ReportFormat> {
        return strategies.keys
    }

    /**
     * Определяет формат отчета по содержимому
     */
    private fun detectFormatFromContent(content: String): ReportFormat {
        val trimmedContent = content.trim()

        return when {
            trimmedContent.startsWith("<?xml") && trimmedContent.contains("testsuite") -> ReportFormat.JUNIT_XML
            trimmedContent.startsWith("{") && trimmedContent.contains("testResults") -> ReportFormat.YAXUNIT_JSON
            trimmedContent.startsWith("{") -> ReportFormat.JSON
            else -> ReportFormat.PLAIN_TEXT
        }
    }
}
