package io.github.alkoleft.mcp.configuration.properties

/**
 * Настройки инструментов
 */
data class ToolsProperties(
    val builder: BuilderType = BuilderType.DESIGNER,
    val edtWorkSpace: String? = null,
    val edtCli: EdtCliProperties = EdtCliProperties(),
)

/**
 * Настройки EDT CLI
 */
data class EdtCliProperties(
    val autoStart: Boolean = false,
    val version: String = "latest",
    val interactiveMode: Boolean = true,
    val workingDirectory: String? = null,
    val startupTimeoutMs: Long = 30000, // 30 секунд по умолчанию
    val commandTimeoutMs: Long = 300000, // 5 минут по умолчанию
    val readyCheckTimeoutMs: Long = 5000, // 5 секунд для проверки готовности
)
