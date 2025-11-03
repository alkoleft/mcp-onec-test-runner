package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.Command

/**
 * Базовый интерфейс для всех команд ibcmd
 *
 * Все команды должны реализовывать этот интерфейс для обеспечения
 * единообразного выполнения и формирования аргументов командной строки.
 */
interface IbcmdCommand : Command {
    /**
     * Имя команды для выполнения (например, "infobase create", "config load")
     */
    val commandName: String

    /**
     * Режим работы ibcmd (infobase, server, config, session, lock, mobile-app, mobile-client, extension)
     */
    val mode: String

    /**
     * Подкоманда в рамках режима (create, load, save, export, import и т.д.)
     */
    val subCommand: String
}
