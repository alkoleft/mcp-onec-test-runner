package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 13. sign — Цифровая подпись конфигурации/расширения
 *
 * Подписывает конфигурацию или расширение.
 */
data class ConfigSignCommand(
    /**
     * Путь к приватному ключу
     * --key=<path>, -k <path>
     */
    var key: String? = null,

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
     * Путь для подписанной копии
     * --out=<path>, -o <path>
     */
    var out: String? = null,

    /**
     * Путь к файлу для подписи
     */
    var path: String? = null
) : IbcmdCommand {

    override val mode: String = "config"
    override val subCommand: String = "sign"
    override val commandName: String = "config sign"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            key?.let { args.addAll(listOf("--key", it)) }
            extension?.let { args.addAll(listOf("--extension", it)) }
            if (db) args.add("--db")
            out?.let { args.addAll(listOf("--out", it)) }
            path?.let { args.add(it) }

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()

        key?.let { details.add("ключ: $it") }
        extension?.let { details.add("расширение: $it") }
        if (db) details.add("конфигурация БД")
        out?.let { details.add("вывод: $it") }
        path?.let { details.add("файл: $it") }

        return "Цифровая подпись конфигурации/расширения" +
                if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}