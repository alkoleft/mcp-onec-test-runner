package io.github.alkoleft.mcp.configuration.properties

/**
 * Настройки инструментов
 */
data class ToolsProperties(
    val builder: BuilderType = BuilderType.DESIGNER,
    val edtVersion: String? = null,
    val edtWorkSpace: String? = null,
)
