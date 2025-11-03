package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 6. repair — Восстановление конфигурации после незавершённой операции
 *
 * Восстанавливает конфигурацию после сбоя операции.
 */
data class ConfigRepairCommand(
    /**
     * Завершить незавершённую операцию
     * --commit
     */
    var commit: Boolean = false,
    /**
     * Отменить незавершённую операцию
     * --rollback
     */
    var rollback: Boolean = false,
    /**
     * Восстановить структуру метаданных
     * --fix-metadata
     */
    var fixMetadata: Boolean = false,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "repair"
    override val commandName: String = "config repair"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            if (commit) args.add("--commit")
            if (rollback) args.add("--rollback")
            if (fixMetadata) args.add("--fix-metadata")

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        if (commit) details.add("завершить операцию")
        if (rollback) details.add("отменить операцию")
        if (fixMetadata) details.add("восстановить метаданные")

        return "Восстановление конфигурации после незавершённой операции" +
                if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
