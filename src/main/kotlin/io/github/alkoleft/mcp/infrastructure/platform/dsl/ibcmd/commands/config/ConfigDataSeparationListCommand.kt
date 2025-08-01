package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 10. data-separation list — Список разделителей информационной базы
 *
 * Выводит список разделителей информационной базы.
 */
class ConfigDataSeparationListCommand(
) : IbcmdCommand {

    override val mode: String = "config"
    override val subCommand: String = "data-separation list"
    override val commandName: String = "config data-separation list"

    override val arguments = emptyList<String>()

    override fun getFullDescription(): String {
        return "Список разделителей информационной базы"
    }
}