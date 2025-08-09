package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.toYesNo

/**
 * 11. extension — Управление расширениями конфигурации
 *
 * Создание, получение информации, список, обновление, удаление расширений.
 * Подкоманды: create, info, list, update, delete
 */
data class ConfigExtensionCommand(
    /**
     * Подкоманда управления расширениями
     * (create, info, list, update, delete)
     */
    var extensionSubCommand: String,
    /**
     * Имя расширения
     * --name=<name>
     */
    var name: String? = null,
    /**
     * Префикс имен
     * --name-prefix=<prefix>
     */
    var namePrefix: String? = null,
    /**
     * Синоним
     * --synonym=<synonym>
     */
    var synonym: String? = null,
    /**
     * Назначение (customization|add-on|patch)
     * --purpose=<customization|add-on|patch>
     */
    var purpose: String? = null,
    /**
     * Активность (yes|no)
     * --active=<yes|no>
     */
    var active: Boolean? = null,
    /**
     * Безопасный режим (yes|no)
     * --safe-mode=<yes|no>
     */
    var safeMode: Boolean? = null,
    /**
     * Профиль безопасности (yes|no)
     * --security-profile-name=<yes|no>
     */
    var securityProfileName: Boolean? = null,
    /**
     * Защита от опасных действий (yes|no)
     * --unsafe-action-protection=<yes|no>
     */
    var unsafeActionProtection: Boolean? = null,
    /**
     * Используется в распределённой ИБ (yes|no)
     * --used-in-distributed-infobase=<yes|no>
     */
    var usedInDistributedInfobase: Boolean? = null,
    /**
     * Область действия (infobase|data-separation)
     * --scope=<infobase|data-separation>
     */
    var scope: String? = null,
    /**
     * Удалить все расширения (только для delete)
     * --all
     */
    var all: Boolean = false,
) : IbcmdCommand {
    override val mode: String = "config"
    override val subCommand: String = "extension $extensionSubCommand"
    override val commandName: String = "config extension $extensionSubCommand"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            args.add("extension")
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
            "create" -> "Создание расширения"
            "info" -> "Информация о расширении"
            "list" -> "Список расширений"
            "update" -> "Обновление расширения"
            "delete" -> "Удаление расширения"
            else -> "Управление расширениями: $extensionSubCommand"
        } + if (details.isNotEmpty()) " (${details.joinToString(", ")})" else ""
    }
}
