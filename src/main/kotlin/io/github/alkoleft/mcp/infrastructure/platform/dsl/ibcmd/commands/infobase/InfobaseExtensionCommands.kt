package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.infobase

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.toYesNo

/**
 * 7. extension — Управление расширениями
 *
 * Создание, получение информации, список, обновление, удаление расширений.
 * Подкоманды: create, info, list, update, delete
 */
data class InfobaseExtensionCommand(
    /**
     * Подкоманда управления расширениями
     * (create, info, list, update, delete)
     */
    val extensionSubCommand: String,

    /**
     * Имя расширения
     * --name=<name>
     */
    val name: String? = null,

    /**
     * Префикс имен
     * --name-prefix=<prefix>
     */
    val namePrefix: String? = null,

    /**
     * Синоним
     * --synonym=<synonym>
     */
    val synonym: String? = null,

    /**
     * Назначение (customization|add-on|patch)
     * --purpose=<customization|add-on|patch>
     */
    val purpose: String? = null,

    /**
     * Активность (yes|no)
     * --active=<yes|no>
     */
    val active: Boolean? = null,

    /**
     * Безопасный режим (yes|no)
     * --safe-mode=<yes|no>
     */
    val safeMode: Boolean? = null,

    /**
     * Профиль безопасности (yes|no)
     * --security-profile-name=<yes|no>
     */
    val securityProfileName: Boolean? = null,

    /**
     * Защита от опасных действий (yes|no)
     * --unsafe-action-protection=<yes|no>
     */
    val unsafeActionProtection: Boolean? = null,

    /**
     * Используется в распределённой ИБ (yes|no)
     * --used-in-distributed-infobase=<yes|no>
     */
    val usedInDistributedInfobase: Boolean? = null,

    /**
     * Область действия (infobase|data-separation)
     * --scope=<infobase|data-separation>
     */
    val scope: String? = null,

    /**
     * Удалить все расширения (только для delete)
     * --all
     */
    val all: Boolean = false
) : IbcmdCommand {

    override val mode: String = "infobase"
    override val subCommand: String = "extension $extensionSubCommand"
    override val commandName: String = "infobase extension $extensionSubCommand"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            args.add(extensionSubCommand)

            name?.let { args.addAll(listOf("--name", it)) }
            namePrefix?.let { args.addAll(listOf("--name-prefix", it)) }
            synonym?.let { args.addAll(listOf("--synonym", it)) }
            purpose?.let { args.addAll(listOf("--purpose", it)) }
            active?.let { args.addAll(listOf("--active", it.toYesNo())) }
            safeMode?.let { args.addAll(listOf("--safe-mode", it.toYesNo())) }
            securityProfileName?.let { args.addAll(listOf("--security-profile-name", it.toYesNo())) }
            unsafeActionProtection?.let { args.addAll(listOf("--unsafe-action-protection", it.toYesNo())) }
            usedInDistributedInfobase?.let { args.addAll(listOf("--used-in-distributed-infobase", it.toYesNo())) }
            scope?.let { args.addAll(listOf("--scope", it)) }
            if (all) args.add("--all")

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()

        name?.let { details.add("имя: $it") }
        namePrefix?.let { details.add("префикс: $it") }
        purpose?.let { details.add("назначение: $it") }
        active?.let { details.add("активность: $it") }
        safeMode?.let { details.add("безопасный режим: $it") }
        scope?.let { details.add("область действия: $it") }
        if (all) details.add("все расширения")

        return when (extensionSubCommand) {
            "create" -> "Создание расширения в ИБ"
            "info" -> "Информация о расширении в ИБ"
            "list" -> "Список расширений в ИБ"
            "update" -> "Обновление расширения в ИБ"
            "delete" -> "Удаление расширения в ИБ"
            else -> "Управление расширениями в ИБ: $extensionSubCommand"
        } + if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}

/**
 * 8. generation-id — Получить идентификатор поколения конфигурации
 *
 * Получает идентификатор поколения конфигурации.
 */
data class InfobaseGenerationIdCommand(
    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    val extension: String? = null
) : IbcmdCommand {

    override val mode: String = "infobase"
    override val subCommand: String = "generation-id"
    override val commandName: String = "infobase generation-id"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            extension?.let { args.addAll(listOf("--extension", it)) }

            return args
        }

    override fun getFullDescription(): String {
        return "Получить идентификатор поколения конфигурации" +
                (extension?.let { " (расширение: $it)" } ?: "")
    }
}

/**
 * 9. sign — Цифровая подпись конфигурации/расширения
 *
 * Подписывает конфигурацию или расширение.
 */
data class InfobaseSignCommand(
    /**
     * Путь к приватному ключу
     * --key=<path>, -k <path>
     */
    val key: String? = null,

    /**
     * Имя расширения
     * --extension=<extension>, -e <extension>
     */
    val extension: String? = null,

    /**
     * Операция над конфигурацией базы данных
     * --db
     */
    val db: Boolean = false,

    /**
     * Путь для подписанной копии
     * --out=<path>, -o <path>
     */
    val out: String? = null,

    /**
     * Путь к файлу для подписи
     */
    val path: String? = null
) : IbcmdCommand {

    override val mode: String = "infobase"
    override val subCommand: String = "sign"
    override val commandName: String = "infobase sign"

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

        return "Цифровая подпись конфигурации/расширения в ИБ" +
                if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
} 