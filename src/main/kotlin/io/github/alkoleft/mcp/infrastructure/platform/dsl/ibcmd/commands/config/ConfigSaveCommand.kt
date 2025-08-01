package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 2. save — Выгрузка конфигурации
 *
 * Выгружает конфигурацию из информационной базы в файл.
 */
data class ConfigSaveCommand(
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    var extension: String? = null,

    /**
     * Операция над конфигурацией базы данных
     * --db
     */
    var db: Boolean = false,

    /**
     * Путь к файлу конфигурации
     */
    val path: String
) : IbcmdCommand {

    override val mode: String = "config"
    override val subCommand: String = "save"
    override val commandName: String = "config save"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            extension?.let { args.addAll(listOf("--extension", it)) }
            if (db) args.add("--db")
            args.add(path)

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        extension?.let { details.add("расширение: $it") }
        if (db) details.add("конфигурация БД")

        return "Выгрузка конфигурации в файл: $path" +
                if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}