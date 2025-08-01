package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 1. load — Загрузка конфигурации
 *
 * Загружает конфигурацию из файла в информационную базу.
 */
data class ConfigLoadCommand(
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
     * Путь к файлу конфигурации
     */
    val path: String
) : IbcmdCommand {

    override val mode: String = "config"
    override val subCommand: String = "load"
    override val commandName: String = "config load"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            extension?.let { args.addAll(listOf("--extension", it)) }
            if (force) args.add("--force")
            args.add(path)

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        extension?.let { details.add("расширение: $it") }
        if (force) details.add("принудительно")

        return "Загрузка конфигурации из файла: $path" +
                if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}

