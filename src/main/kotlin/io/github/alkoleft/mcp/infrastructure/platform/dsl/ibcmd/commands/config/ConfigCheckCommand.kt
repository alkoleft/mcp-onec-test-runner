package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 3. check — Проверка конфигурации
 *
 * Проверяет корректность конфигурации.
 */
data class ConfigCheckCommand(
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    var extension: String? = null,
    /**
     * Подтверждение при наличии предупреждений
     * --force, -F
     */
    var force: Boolean = false,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "check"
    override val commandName: String = "config check"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            extension?.let { args.addAll(listOf("--extension", it)) }
            if (force) args.add("--force")

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        extension?.let { details.add("расширение: $it") }
        if (force) details.add("принудительно")

        return "Проверка конфигурации" +
                if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
