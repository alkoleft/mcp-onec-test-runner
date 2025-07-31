package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.executor.ProcessExecutor
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * DSL для работы с конфигуратором 1С:Предприятие
 *
 * Предоставляет удобный интерфейс для выполнения операций конфигуратора
 * через fluent API и DSL синтаксис.
 */
class ConfiguratorDsl(
    private val context: PlatformUtilityContext
) {
    private val configuratorContext = ConfiguratorContext(context)

    /**
     * Устанавливает строку подключения к информационной базе
     */
    fun connect(connectionString: String) {
        configuratorContext.connect(connectionString)
    }

    /**
     * Устанавливает строку подключения к серверной информационной базе
     */
    fun connectToServer(serverName: String, dbName: String) {
        configuratorContext.connectToServer(serverName, dbName)
    }

    /**
     * Устанавливает строку подключения к файловой информационной базе
     */
    fun connectToFile(path: String) {
        configuratorContext.connectToFile(path)
    }

    /**
     * Устанавливает пользователя для подключения
     */
    fun user(user: String) {
        configuratorContext.user(user)
    }

    /**
     * Устанавливает пароль для подключения
     */
    fun password(password: String) {
        configuratorContext.password(password)
    }

    /**
     * Устанавливает путь к конфигурации (для операций с файлами .cf)
     */
    fun config(path: Path) {
        configuratorContext.config(path)
    }

    /**
     * Устанавливает путь для вывода
     */
    fun output(path: Path) {
        configuratorContext.output(path)
    }

    /**
     * Устанавливает путь для лог-файла
     */
    fun log(path: Path) {
        configuratorContext.log(path)
    }

    /**
     * Устанавливает код языка интерфейса
     */
    fun language(code: String) {
        configuratorContext.language(code)
    }

    /**
     * Устанавливает код локализации сеанса
     */
    fun localization(code: String) {
        configuratorContext.localization(code)
    }

    /**
     * Устанавливает скорость соединения
     */
    fun connectionSpeed(speed: ConnectionSpeed) {
        configuratorContext.connectionSpeed(speed)
    }

    /**
     * Отключает диалоговые окна запуска
     */
    fun disableStartupDialogs() {
        configuratorContext.disableStartupDialogs()
    }

    /**
     * Отключает сообщения запуска
     */
    fun disableStartupMessages() {
        configuratorContext.disableStartupMessages()
    }

    /**
     * Не очищает файл вывода при записи
     */
    fun noTruncate() {
        configuratorContext.noTruncate()
    }

    /**
     * Добавляет дополнительные параметры
     */
    fun param(param: String) {
        configuratorContext.param(param)
    }

    /**
     * Загружает конфигурацию из каталога с файлами исходников
     */
    fun loadFromFiles(sourcePath: Path): ConfiguratorResult {
        return runBlocking {
            val command = LoadConfigFromFilesCommand(sourcePath = sourcePath)
            executeConfiguratorCommandWithArgs(command.name, command.arguments)
        }
    }

    /**
     * Загружает основную конфигурацию из файла
     */
    fun loadMainConfig(configPath: Path): ConfiguratorResult {
        return runBlocking {
            executeConfiguratorCommand("LoadCfg", configPath)
        }
    }

    /**
     * Загружает расширение конфигурации из файла
     */
    fun loadExtension(extensionPath: Path): ConfiguratorResult {
        return runBlocking {
            executeConfiguratorCommand("LoadCfg", extensionPath)
        }
    }

    /**
     * Проверяет возможность применения расширений конфигурации
     */
    fun checkCanApplyExtensions(): ConfiguratorResult {
        return runBlocking {
            executeConfiguratorCommand("CheckCanApplyConfigurationExtensions")
        }
    }

    /**
     * Обновляет конфигурацию в информационной базе
     */
    fun updateDatabaseConfig(): ConfiguratorResult {
        return runBlocking {
            executeConfiguratorCommand("UpdateDBCfg")
        }
    }

    /**
     * Проверяет конфигурацию
     */
    fun checkConfig(): ConfiguratorResult {
        return runBlocking {
            executeConfiguratorCommand("CheckConfig")
        }
    }

    /**
     * Проверяет модули конфигурации
     */
    fun checkModules(): ConfiguratorResult {
        return runBlocking {
            executeConfiguratorCommand("CheckModules")
        }
    }


    /**
     * Выполняет произвольную команду конфигуратора
     */
    fun command(command: String): ConfiguratorResult {
        return runBlocking {
            executeConfiguratorCommand(command)
        }
    }

    /**
     * Выполняет команду конфигуратора с синхронным API
     */
    fun commandSync(command: String): ConfiguratorResult {
        return runBlocking {
            executeConfiguratorCommand(command)
        }
    }

    /**
     * Строит результат выполнения операций
     */
    fun buildResult(): ConfiguratorResult {
        return ConfiguratorResult(
            success = true,
            output = "",
            error = null,
            exitCode = 0,
            duration = Duration.ZERO
        )
    }

    /**
     * Выполняет команду конфигуратора с произвольными аргументами
     */
    private suspend fun executeConfiguratorCommandWithArgs(
        command: String,
        commandArgs: List<String>
    ): ConfiguratorResult {
        val duration = measureTime {
            try {
                val executor = ProcessExecutor()

                val args = buildCommandArgsWithArgs(command, commandArgs)
                val result = executor.execute(args)

                configuratorContext.setResult(
                    success = result.exitCode == 0,
                    output = result.output,
                    error = result.error,
                    exitCode = result.exitCode,
                    duration = result.duration
                )

            } catch (e: Exception) {
                configuratorContext.setResult(
                    success = false,
                    output = "",
                    error = e.message ?: "Unknown error",
                    exitCode = -1,
                    duration = Duration.ZERO
                )
            }
        }

        return ConfiguratorResult(
            success = configuratorContext.buildResult().success,
            output = configuratorContext.buildResult().output,
            error = configuratorContext.buildResult().error,
            exitCode = configuratorContext.buildResult().exitCode,
            duration = duration
        )
    }

    /**
     * Выполняет команду конфигуратора
     */
    private suspend fun executeConfiguratorCommand(
        command: String,
        filePath: Path? = null
    ): ConfiguratorResult {
        val duration = measureTime {
            try {
                val executor = ProcessExecutor()

                val commandArgs = buildCommandArgs(command, filePath)
                val result = executor.execute(commandArgs)

                configuratorContext.setResult(
                    success = result.exitCode == 0,
                    output = result.output,
                    error = result.error,
                    exitCode = result.exitCode,
                    duration = result.duration
                )

            } catch (e: Exception) {
                configuratorContext.setResult(
                    success = false,
                    output = "",
                    error = e.message ?: "Unknown error",
                    exitCode = -1,
                    duration = Duration.ZERO
                )
            }
        }

        return ConfiguratorResult(
            success = configuratorContext.buildResult().success,
            output = configuratorContext.buildResult().output,
            error = configuratorContext.buildResult().error,
            exitCode = configuratorContext.buildResult().exitCode,
            duration = duration
        )
    }

    /**
     * Строит аргументы команды для конфигуратора с произвольными аргументами
     */
    private suspend fun buildCommandArgsWithArgs(
        command: String,
        commandArgs: List<String>
    ): List<String> {
        val args = mutableListOf<String>()

        // Базовые аргументы конфигуратора
        args.addAll(configuratorContext.buildBaseArgs())

        // Команда и её аргументы (команда уже включена в commandArgs)
        args.addAll(commandArgs)

        return args
    }

    /**
     * Строит аргументы команды для конфигуратора
     */
    private suspend fun buildCommandArgs(
        command: String,
        filePath: Path? = null
    ): List<String> {
        val args = mutableListOf<String>()

        // Базовые аргументы конфигуратора
        args.addAll(configuratorContext.buildBaseArgs())

        // Команда
        args.add(command)

        // Путь к файлу (если указан)
        filePath?.let { args.add(it.toString()) }

        return args
    }
}

/**
 * Скорость соединения
 */
enum class ConnectionSpeed(val value: String) {
    NORMAL("Normal"),
    LOW("Low")
}

/**
 * Результат выполнения операций с конфигуратором
 */
data class ConfiguratorResult(
    val success: Boolean,
    val output: String,
    val error: String?,
    val exitCode: Int,
    val duration: Duration
) 