package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.mobile

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common.IbcmdCommand

/**
 * 1. export — Экспорт мобильного приложения
 *
 * Экспортирует мобильное приложение для развертывания.
 */
data class MobileAppExportCommand(
    /**
     * Путь для экспорта мобильного приложения
     */
    val path: String
) : IbcmdCommand {

    override val mode: String = "mobile-app"
    override val subCommand: String = "export"
    override val commandName: String = "mobile-app export"

    override val arguments = listOf(path)

    override fun getFullDescription(): String {
        return "Экспорт мобильного приложения в: $path"
    }
} 