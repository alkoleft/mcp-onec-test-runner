package io.github.alkoleft.mcp.configuration.properties

/**
 * Настройки подключения к информационной базе
 */
data class ConnectionProperties(
    val connectionString: String,
    val user: String? = null,
    val password: String? = null
)