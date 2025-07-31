package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Контекст для работы с конфигуратором 1С
 *
 * Содержит базовые настройки и методы для построения аргументов команд
 */
class ConfiguratorContext(
    private val platformContext: PlatformUtilityContext
) {
    private var connectionString: String = ""
    private var user: String? = null
    private var password: String? = null
    private var configPath: Path? = null
    private var outputPath: Path? = null
    private var logPath: Path? = null
    private var language: String? = null
    private var localization: String? = null
    private var connectionSpeed: ConnectionSpeed? = null
    private var disableStartupDialogs: Boolean = false
    private var disableStartupMessages: Boolean = false
    private var noTruncate: Boolean = false
    private var additionalParams: MutableList<String> = mutableListOf()

    /**
     * Устанавливает строку подключения к информационной базе
     */
    fun connect(connectionString: String) {
        this.connectionString = "\"$connectionString\""
    }

    /**
     * Устанавливает строку подключения к серверной информационной базе
     */
    fun connectToServer(serverName: String, dbName: String) {
        this.connectionString = "\"Srvr=\"\"$serverName\"\";Ref=\"\"$dbName\"\";\""
    }

    /**
     * Устанавливает строку подключения к файловой информационной базе
     */
    fun connectToFile(path: String) {
        this.connectionString = "\"File=\"\"$path\"\";\""
    }

    /**
     * Устанавливает пользователя для подключения
     */
    fun user(user: String) {
        this.user = user
    }

    /**
     * Устанавливает пароль для подключения
     */
    fun password(password: String) {
        this.password = password
    }

    /**
     * Устанавливает путь к конфигурации (для операций с файлами .cf)
     */
    fun config(path: Path) {
        this.configPath = path
    }

    /**
     * Устанавливает путь для вывода
     */
    fun output(path: Path) {
        this.outputPath = path
    }

    /**
     * Устанавливает путь для лог-файла
     */
    fun log(path: Path) {
        this.logPath = path
    }

    /**
     * Устанавливает код языка интерфейса
     */
    fun language(code: String) {
        this.language = code
    }

    /**
     * Устанавливает код локализации сеанса
     */
    fun localization(code: String) {
        this.localization = code
    }

    /**
     * Устанавливает скорость соединения
     */
    fun connectionSpeed(speed: ConnectionSpeed) {
        this.connectionSpeed = speed
    }

    /**
     * Отключает диалоговые окна запуска
     */
    fun disableStartupDialogs() {
        this.disableStartupDialogs = true
    }

    /**
     * Отключает сообщения запуска
     */
    fun disableStartupMessages() {
        this.disableStartupMessages = true
    }

    /**
     * Не очищает файл вывода при записи
     */
    fun noTruncate() {
        this.noTruncate = true
    }

    /**
     * Добавляет дополнительные параметры
     */
    fun param(param: String) {
        additionalParams.add(param)
    }

    /**
     * Строит базовые аргументы для команд конфигуратора
     */
    suspend fun buildBaseArgs(): List<String> {
        val args = mutableListOf<String>()

        // Путь к исполняемому файлу конфигуратора
        val location = platformContext.locateUtility(UtilityType.DESIGNER)
        args.add(location.executablePath.toString())
        args.add("DESIGNER")

        // Строка подключения
        if (connectionString.isNotEmpty()) {
            args.add("/IBConnectionString")
            args.add(connectionString)
        }

        // Пользователь
        user?.let { args.add("/N$it") }

        // Пароль
        password?.let { args.add("/P$it") }

        // Путь к конфигурации (для операций с .cf файлами)
        configPath?.let { args.add("/CF$it") }

        // Путь для вывода
        outputPath?.let {
            args.add("/Out$it")
            if (noTruncate) {
                args.add("-NoTruncate")
            }
        }

        // Путь для лог-файла
        logPath?.let { args.add("/LogFile$it") }

        // Код языка
        language?.let { args.add("/L$it") }

        // Код локализации
        localization?.let { args.add("/VL$it") }

        // Скорость соединения
        connectionSpeed?.let { args.add("/O$it") }

        // Отключение диалогов
        if (disableStartupDialogs) {
            args.add("/DisableStartupDialogs")
        }

        // Отключение сообщений
        if (disableStartupMessages) {
            args.add("/DisableStartupMessages")
        }

        // Дополнительные параметры
        args.addAll(additionalParams)

        return args
    }

    /**
     * Устанавливает результат выполнения
     */
    fun setResult(
        success: Boolean,
        output: String,
        error: String?,
        exitCode: Int,
        duration: Duration
    ) {
        platformContext.setResult(success, output, error, exitCode, duration)
    }

    /**
     * Строит результат выполнения
     */
    fun buildResult() = platformContext.buildResult()
} 