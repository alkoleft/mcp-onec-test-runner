package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 9. support disable — Снятие конфигурации с поддержки
 *
 * Снимает конфигурацию с поддержки.
 */
data class ConfigSupportDisableCommand(
    /**
     * Снятие с поддержки принудительно
     * --force, -F
     */
    var force: Boolean = false,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "support disable"
    override val commandName: String = "config support disable"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            if (force) args.add("--force")

            return args
        }

    override fun getFullDescription(): String = "Снятие конфигурации с поддержки" + if (force) " (принудительно)" else ""
}
