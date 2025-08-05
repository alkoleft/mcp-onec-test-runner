package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator

import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.ApplyCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.CheckCanApplyConfigurationExtensionsCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.CheckConfigCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.CheckModulesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.CreateCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.DeleteCfgCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.DumpConfigToFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.DumpExtensionToFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.LoadConfigFromFilesCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.UpdateDBCfgCommand
import java.nio.file.Path

/**
 * Интерфейс для DSL конфигуратора 1С:Предприятие
 *
 * Определяет общий интерфейс для работы с конфигуратором через fluent API
 */
interface ConfiguratorDslInterface<T> {
    /**
     * Устанавливает строку подключения к информационной базе
     */
    fun connect(connectionString: String)

    /**
     * Устанавливает строку подключения к серверной информационной базе
     */
    fun connectToServer(serverName: String, dbName: String)

    /**
     * Устанавливает строку подключения к файловой информационной базе
     */
    fun connectToFile(path: String)

    /**
     * Устанавливает пользователя для подключения
     */
    fun user(user: String)

    /**
     * Устанавливает пароль для подключения
     */
    fun password(password: String)

    /**
     * Устанавливает путь к конфигурации (для операций с файлами .cf)
     */
    fun config(path: Path)

    /**
     * Устанавливает путь для вывода
     */
    fun output(path: Path)

    /**
     * Устанавливает путь для лог-файла
     */
    fun log(path: Path)

    /**
     * Устанавливает код языка интерфейса
     */
    fun language(code: String)

    /**
     * Устанавливает код локализации сеанса
     */
    fun localization(code: String)

    /**
     * Устанавливает скорость соединения
     */
    fun connectionSpeed(speed: ConnectionSpeed)

    /**
     * Отключает диалоговые окна запуска
     */
    fun disableStartupDialogs()

    /**
     * Отключает сообщения запуска
     */
    fun disableStartupMessages()

    /**
     * Не очищает файл вывода при записи
     */
    fun noTruncate()

    /**
     * Добавляет дополнительные параметры
     */
    fun param(param: String)

    /**
     * Загружает конфигурацию из каталога с файлами исходников
     */
    fun loadConfigFromFiles(block: LoadConfigFromFilesCommand.() -> Unit): T

    /**
     * Обновляет конфигурацию в информационной базе
     */
    fun updateDBCfg(block: UpdateDBCfgCommand.() -> Unit): T

    /**
     * Проверяет возможность применения расширений конфигурации
     */
    fun checkCanApplyConfigurationExtensions(block: CheckCanApplyConfigurationExtensionsCommand.() -> Unit): T

    /**
     * Проверяет конфигурацию
     */
    fun checkConfig(block: CheckConfigCommand.() -> Unit): T

    /**
     * Проверяет модули конфигурации
     */
    fun checkModules(block: CheckModulesCommand.() -> Unit): T

    /**
     * Выгружает конфигурацию в каталог с файлами
     */
    fun dumpConfigToFiles(block: DumpConfigToFilesCommand.() -> Unit): T

    /**
     * Выгружает расширение конфигурации в каталог с файлами
     */
    fun dumpExtensionToFiles(block: DumpExtensionToFilesCommand.() -> Unit): T

    /**
     * Применяет расширение конфигурации
     */
    fun applyCfg(block: ApplyCfgCommand.() -> Unit): T

    /**
     * Создает расширение конфигурации
     */
    fun createCfg(block: CreateCfgCommand.() -> Unit): T

    /**
     * Удаляет расширение конфигурации
     */
    fun deleteCfg(block: DeleteCfgCommand.() -> Unit): T
}