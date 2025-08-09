package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 5. reset — Возврат к конфигурации базы данных
 *
 * Возвращает конфигурацию к состоянию базы данных.
 */
data class ConfigResetCommand(
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    var extension: String? = null,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "reset"
    override val commandName: String = "config reset"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            extension?.let { args.addAll(listOf("--extension", it)) }

            return args
        }

    override fun getFullDescription(): String =
        "Возврат к конфигурации базы данных" +
            (extension?.let { " (расширение: $it)" } ?: "")
}
