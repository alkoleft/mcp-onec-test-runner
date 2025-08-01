package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.lock

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 1. list — Получение списка блокировок
 *
 * Получает список всех активных блокировок в информационной базе.
 */
data class LockListCommand(
    /**
     * Идентификатор сеанса (опционально, для фильтрации по сеансу)
     * --session=<uuid>
     */
    val session: String? = null
) : IbcmdCommand {

    override val mode: String = "lock"
    override val subCommand: String = "list"
    override val commandName: String = "lock list"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            session?.let { args.addAll(listOf("--session", it)) }

            return args
        }

    override fun getFullDescription(): String {
        return "Получение списка блокировок" +
                (session?.let { " (сеанс: $it)" } ?: "")
    }
} 