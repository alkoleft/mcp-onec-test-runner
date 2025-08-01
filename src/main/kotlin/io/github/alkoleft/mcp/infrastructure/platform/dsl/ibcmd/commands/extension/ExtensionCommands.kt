package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.extension

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.toYesNo

/**
 * 1. create — Создание расширения
 *
 * Создаёт новое расширение конфигурации.
 */
data class ExtensionCreateCommand(
    /**
     * Имя расширения (обязательно, должно начинаться с буквы, содержать только буквы, цифры и "_")
     * --name=<name>
     */
    val name: String,

    /**
     * Префикс имен (обязательно, правила те же)
     * --name-prefix=<prefix>
     */
    val namePrefix: String,

    /**
     * Синоним в формате функции NStr()
     * --synonym=<synonym>
     */
    var synonym: String? = null,

    /**
     * Назначение расширения (customization|add-on|patch)
     * --purpose=<customization|add-on|patch>
     */
    var purpose: String? = null
) : IbcmdCommand {

    override val mode: String = "extension"
    override val subCommand: String = "create"
    override val commandName: String = "extension create"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            args.addAll(listOf("--name", name))
            args.addAll(listOf("--name-prefix", namePrefix))
            synonym?.let { args.addAll(listOf("--synonym", it)) }
            purpose?.let { args.addAll(listOf("--purpose", it)) }

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        details.add("имя: $name")
        details.add("префикс: $namePrefix")
        synonym?.let { details.add("синоним: $it") }
        purpose?.let { details.add("назначение: $it") }

        return "Создание расширения (${details.joinToString(", ")})"
    }
}

/**
 * 2. info — Получение информации о расширении
 *
 * Получает подробную информацию о конкретном расширении.
 */
data class ExtensionInfoCommand(
    /**
     * Имя расширения (обязательно)
     * --name=<name>
     */
    val name: String
) : IbcmdCommand {

    override val mode: String = "extension"
    override val subCommand: String = "info"
    override val commandName: String = "extension info"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            args.addAll(listOf("--name", name))

            return args
        }

    override fun getFullDescription(): String {
        return "Получение информации о расширении: $name"
    }
}

/**
 * 3. list — Получение списка расширений
 *
 * Получает список всех расширений в информационной базе.
 */
class ExtensionListCommand() : IbcmdCommand {

    override val mode: String = "extension"
    override val subCommand: String = "list"
    override val commandName: String = "extension list"

    override val arguments: List<String> = emptyList()

    override fun getFullDescription(): String {
        return "Получение списка расширений"
    }
}

/**
 * 4. update — Обновление свойств расширения
 *
 * Обновляет свойства указанного расширения.
 */
data class ExtensionUpdateCommand(
    /**
     * Имя расширения (обязательно)
     * --name=<name>
     */
    val name: String,

    /**
     * Активность расширения (yes|no)
     * --active=<yes|no>
     */
    var active: Boolean? = null,

    /**
     * Безопасный режим (yes|no)
     * --safe-mode=<yes|no>
     */
    var safeMode: Boolean? = null,

    /**
     * Имя профиля безопасности (yes|no)
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
     * Область действия расширения (infobase|data-separation)
     * --scope=<infobase|data-separation>
     */
    var scope: String? = null
) : IbcmdCommand {

    override val mode: String = "extension"
    override val subCommand: String = "update"
    override val commandName: String = "extension update"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            args.addAll(listOf("--name", name))
            active?.let { args.addAll(listOf("--active", it.toYesNo())) }
            safeMode?.let { args.addAll(listOf("--safe-mode", it.toYesNo())) }
            securityProfileName?.let { args.addAll(listOf("--security-profile-name", it.toYesNo())) }
            unsafeActionProtection?.let { args.addAll(listOf("--unsafe-action-protection", it.toYesNo())) }
            usedInDistributedInfobase?.let { args.addAll(listOf("--used-in-distributed-infobase", it.toYesNo())) }
            scope?.let { args.addAll(listOf("--scope", it)) }

            return args
        }

    override fun getFullDescription(): String {
        val details = mutableListOf<String>()
        details.add("имя: $name")
        active?.let { details.add("активность: $it") }
        safeMode?.let { details.add("безопасный режим: $it") }
        scope?.let { details.add("область действия: $it") }

        return "Обновление свойств расширения (${details.joinToString(", ")})"
    }
}

/**
 * 5. delete — Удаление расширения
 *
 * Удаляет указанное расширение или все расширения.
 */
data class ExtensionDeleteCommand(
    /**
     * Имя расширения для удаления
     * --name=<name>
     */
    var name: String? = null,

    /**
     * Удалить все расширения
     * --all
     */
    var all: Boolean = false
) : IbcmdCommand {

    override val mode: String = "extension"
    override val subCommand: String = "delete"
    override val commandName: String = "extension delete"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            name?.let { args.addAll(listOf("--name", it)) }
            if (all) args.add("--all")

            return args
        }

    override fun getFullDescription(): String {
        return if (all) {
            "Удаление всех расширений"
        } else {
            "Удаление расширения: ${name ?: "не указано"}"
        }
    }
} 