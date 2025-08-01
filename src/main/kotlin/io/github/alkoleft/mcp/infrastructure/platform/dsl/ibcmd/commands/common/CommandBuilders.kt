package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigApplyCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigCheckCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigDataSeparationListCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigExportCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigExtensionCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigGenerationIdCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigImportCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigLoadCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigRepairCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigResetCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigSaveCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigSignCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigSupportDisableCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.extension.ExtensionCreateCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.extension.ExtensionDeleteCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.extension.ExtensionInfoCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.extension.ExtensionListCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.extension.ExtensionUpdateCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.infobase.InfobaseClearCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.infobase.InfobaseCreateCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.infobase.InfobaseDumpCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.infobase.InfobaseReplicateCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.infobase.InfobaseRestoreCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.session.SessionInfoCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.session.SessionListCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.session.SessionTerminateCommand

sealed class CommandBuilder {
    protected val commands = mutableListOf<IbcmdCommand>()

    val result
        get() = commands.toList()

    protected fun <T : IbcmdCommand> configureCommand(command: T, configure: (T.() -> Unit)?) {
        commands.add(command.also { if (configure != null) it.configure() })
    }
}

/**
 * Builder для создания команд режима infobase
 */
class InfobaseCommandBuilder() : CommandBuilder() {
    /**
     * Создает команду создания информационной базы
     */
    fun create(configure: InfobaseCreateCommand.() -> Unit = { }) =
        configureCommand(InfobaseCreateCommand(), configure)

    /**
     * Создает команду выгрузки данных ИБ
     */
    fun dump(path: String, configure: InfobaseDumpCommand.() -> Unit = { }) =
        configureCommand(InfobaseDumpCommand(path = path), configure)

    /**
     * Создает команду загрузки данных ИБ
     */
    fun restore(path: String, configure: InfobaseRestoreCommand.() -> Unit = { }) =
        configureCommand(InfobaseRestoreCommand(path = path), configure)

    /**
     * Создает команду очистки ИБ
     */
    fun clear(configure: InfobaseClearCommand.() -> Unit = { }) =
        configureCommand(InfobaseClearCommand(), configure)

    /**
     * Создает команду репликации ИБ
     */
    fun replicate(configure: InfobaseReplicateCommand.() -> Unit = { }) =
        configureCommand(InfobaseReplicateCommand(), configure)
}

/**
 * Builder для создания команд режима config
 */
class ConfigCommandBuilder : CommandBuilder() {
    /**
     * Создает команду загрузки конфигурации
     */
    fun load(path: String, configure: ConfigLoadCommand.() -> Unit = { }) =
        configureCommand(ConfigLoadCommand(path = path), configure)

    /**
     * Создает команду выгрузки конфигурации
     */
    fun save(path: String, configure: ConfigSaveCommand.() -> Unit = { }) =
        configureCommand(ConfigSaveCommand(path = path), configure)

    /**
     * Создает команду проверки конфигурации
     */
    fun check(configure: ConfigCheckCommand.() -> Unit = { }) =
        configureCommand(ConfigCheckCommand(), configure)

    /**
     * Создает команду применения конфигурации
     */
    fun apply(configure: ConfigApplyCommand.() -> Unit = { }) =
        configureCommand(ConfigApplyCommand(), configure)

    /**
     * Создает команду сброса конфигурации
     */
    fun reset(configure: ConfigResetCommand.() -> Unit = { }) =
        configureCommand(ConfigResetCommand(), configure)

    /**
     * Создает команду экспорта конфигурации в XML
     */
    fun export(path: String, configure: ConfigExportCommand.() -> Unit = { }) =
        configureCommand(ConfigExportCommand(path = path), configure)

    /**
     * Создает команду импорта конфигурации из XML
     */
    fun import(path: String, configure: ConfigImportCommand.() -> Unit = {}) =
        configureCommand(ConfigImportCommand(path = path), configure)

    /**
     * Подписывает конфигурацию или расширение.
     */
    fun sign(configure: ConfigSignCommand.() -> Unit = {}) =
        configureCommand(ConfigSignCommand(), configure)

    /**
     * Выводит список разделителей информационной базы.
     */
    fun dataSeparationList(configure: ConfigDataSeparationListCommand.() -> Unit = {}) =
        configureCommand(ConfigDataSeparationListCommand(), configure)

    /**
     * Восстанавливает конфигурацию после сбоя операции.
     */
    fun repair(configure: ConfigRepairCommand.() -> Unit = {}) =
        configureCommand(ConfigRepairCommand(), configure)

    /**
     * Снимает конфигурацию с поддержки.
     */
    fun supportDisable(configure: ConfigSupportDisableCommand.() -> Unit = {}) =
        configureCommand(ConfigSupportDisableCommand(), configure)

    /**
     * Получает идентификатор поколения конфигурации.
     */
    fun generationId(configure: ConfigGenerationIdCommand.() -> Unit = {}) =
        configureCommand(ConfigGenerationIdCommand(), configure)

    /**
     * Создание, получение информации, список, обновление, удаление расширений.
     *  * Подкоманды: create, info, list, update, delete
     */
    fun extension(name: String, subCommand: String, configure: ConfigExtensionCommand.() -> Unit = {}) =
        configureCommand(ConfigExtensionCommand(name = name, extensionSubCommand = subCommand), configure)
}

/**
 * Builder для создания команд режима session
 */
class SessionCommandBuilder() : CommandBuilder() {

    /**
     * Создает команду получения информации о сеансе
     */
    fun info(session: String, configure: SessionInfoCommand.() -> Unit = { }) =
        configureCommand(SessionInfoCommand(session = session), configure)

    /**
     * Создает команду получения списка сеансов
     */
    fun list(configure: SessionListCommand.() -> Unit = { }) = configureCommand(SessionListCommand(), configure)

    /**
     * Создает команду завершения сеанса
     */
    fun terminate(session: String, configure: SessionTerminateCommand.() -> Unit = { }) =
        configureCommand(SessionTerminateCommand(session = session), configure)
}

/**
 * Builder для создания команд режима extension
 */
class ExtensionCommandBuilder() : CommandBuilder() {

    /**
     * Создает команду создания расширения
     */
    fun create(name: String, namePrefix: String, configure: ExtensionCreateCommand.() -> Unit = { }) =
        configureCommand(ExtensionCreateCommand(name = name, namePrefix = namePrefix), configure)

    /**
     * Создает команду получения информации о расширении
     */
    fun info(name: String, configure: ExtensionInfoCommand.() -> Unit = { }) =
        configureCommand(ExtensionInfoCommand(name = name), configure)

    /**
     * Создает команду получения списка расширений
     */
    fun list(configure: ExtensionListCommand.() -> Unit = { }) =
        configureCommand(ExtensionListCommand(), configure)

    /**
     * Создает команду обновления расширения
     */
    fun update(name: String, configure: ExtensionUpdateCommand.() -> Unit = { }) =
        configureCommand(ExtensionUpdateCommand(name = name), configure)

    /**
     * Создает команду удаления расширения
     */
    fun delete(configure: ExtensionDeleteCommand.() -> Unit = { }) =
        configureCommand(ExtensionDeleteCommand(), configure)
}
