package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.Command

/**
 * Базовая команда конфигуратора с DSL функциональностью
 */
abstract class DesignerCommand : Command {
    abstract val name: String
    abstract val description: String
    abstract val parameters: Map<String, String>

    /**
     * Полное описание команды для отображения в плане
     */
    override fun getFullDescription(): String {
        val params =
            if (parameters.isNotEmpty()) {
                " (${parameters.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
            } else {
                ""
            }
        return "$description: $name$params"
    }
}
