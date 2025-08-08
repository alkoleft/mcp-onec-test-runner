package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands

/**
 * Базовая команда конфигуратора с DSL функциональностью
 */
abstract class ConfiguratorCommand {
    abstract val name: String
    abstract val description: String
    abstract val arguments: List<String>
    abstract val parameters: Map<String, String>

    /**
     * Полное описание команды для отображения в плане
     */
    fun getFullDescription(): String {
        val params = if (parameters.isNotEmpty()) {
            " (${parameters.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
        } else ""
        return "$name$params - $description"
    }
}

