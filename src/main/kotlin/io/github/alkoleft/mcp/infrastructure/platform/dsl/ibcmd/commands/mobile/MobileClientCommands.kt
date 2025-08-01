package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.mobile

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 1. export — Экспорт мобильного клиента
 *
 * Экспортирует мобильный клиент для развертывания.
 */
data class MobileClientExportCommand(
    /**
     * Путь для экспорта мобильного клиента
     */
    val path: String
) : IbcmdCommand {

    override val mode: String = "mobile-client"
    override val subCommand: String = "export"
    override val commandName: String = "mobile-client export"

    override val arguments = listOf(path)

    override fun getFullDescription(): String {
        return "Экспорт мобильного клиента в: $path"
    }
}

/**
 * 2. sign — Цифровая подпись мобильного клиента
 *
 * Подписывает мобильный клиент цифровой подписью.
 */
data class MobileClientSignCommand(
    /**
     * Путь к приватному ключу (обязательно, формат .pem)
     * --key=<path>, -k <path>
     */
    val key: String
) : IbcmdCommand {

    override val mode: String = "mobile-client"
    override val subCommand: String = "sign"
    override val commandName: String = "mobile-client sign"

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            args.addAll(listOf("--key", key))

            return args
        }

    override fun getFullDescription(): String {
        return "Цифровая подпись мобильного клиента (ключ: $key)"
    }
} 