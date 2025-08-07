package io.github.alkoleft.mcp.core.modules.strategy

import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.ReportFormat
import java.io.InputStream

/**
 * Стратегия парсинга отчетов о тестировании
 * Поддерживает различные форматы отчетов через Strategy Pattern
 */
interface ReportParserStrategy {
    /**
     * Парсит отчет из входного потока
     */
    suspend fun parse(input: InputStream): GenericTestReport

    /**
     * Проверяет, может ли стратегия обработать указанный формат
     */
    fun canHandle(format: ReportFormat): Boolean

    /**
     * Возвращает поддерживаемые форматы
     */
    fun getSupportedFormats(): Set<ReportFormat>

    /**
     * Определяет формат отчета из содержимого
     */
    suspend fun detectFormat(input: InputStream): ReportFormat
}
