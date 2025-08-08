package io.github.alkoleft.mcp.infrastructure.yaxunit

import io.github.alkoleft.mcp.core.modules.ReportFormat

class ReportParserFactory {
    fun createStrategy(format: ReportFormat): ReportParserStrategy {
        throw UnsupportedOperationException("Report parsing strategies are removed. Use EnhancedReportParser (JUnit only).")
    }

    fun getSupportedFormats(): Set<ReportFormat> = setOf(ReportFormat.JUNIT_XML)
}
