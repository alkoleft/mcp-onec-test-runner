package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 12. generation-id — Идентификатор поколения конфигурации
 *
 * Получает идентификатор поколения конфигурации.
 */
data class ConfigGenerationIdCommand(
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    var extension: String? = null,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "generation-id"
    override val commandName: String = "config generation-id"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()
            extension?.let { args.addAll(listOf("--extension", it)) }

            return args
        }

    override fun getFullDescription(): String =
        "Идентификатор поколения конфигурации" +
            (extension?.let { " (расширение: $it)" } ?: "")
}
