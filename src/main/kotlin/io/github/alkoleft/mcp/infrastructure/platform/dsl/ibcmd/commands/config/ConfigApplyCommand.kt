package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 4. apply — Обновление конфигурации базы данных
 *
 * Применяет конфигурацию к базе данных.
 */
data class ConfigApplyCommand(
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

    /**
     * Динамическое обновление
     * --dynamic=<auto|disable|prompt|force>
     */
    var dynamic: String? = null,

    /**
     * Завершение сеансов
     * --session-terminate=<disable|prompt|force>
     */
    var sessionTerminate: String? = null
) : IbcmdCommand {

    override val mode: String = "config"
    override val subCommand: String = "apply"
    override val commandName: String = "config apply"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()
            extension?.let { args.addAll(listOf("--extension", it)) }
            if (force) args.add("--force")
            dynamic?.let { args.addAll(listOf("--dynamic", it)) }
            sessionTerminate?.let { args.addAll(listOf("--session-terminate", it)) }

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        extension?.let { details.add("расширение: $it") }
        dynamic?.let { details.add("динамическое обновление: $it") }
        sessionTerminate?.let { details.add("завершение сеансов: $it") }
        if (force) details.add("принудительно")

        return "Обновление конфигурации базы данных" +
                if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}