package io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Контекст для работы с 1С:Предприятие
 */
class EnterpriseContext(
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
    private var disableStartupDialogs: Boolean = false
    private var disableStartupMessages: Boolean = false
    private var additionalParams: MutableList<String> = mutableListOf()

    /**
     * Устанавливает строку подключения к информационной базе
     */
    fun connect(connectionString: String) {
        this.connectionString = connectionString
    }

    /**
     * Устанавливает строку подключения к серверной информационной базе
     */
    fun connectToServer(serverName: String, dbName: String) {
        this.connectionString = "Srvr=\"$serverName\";Ref=\"$dbName\";"
    }

    /**
     * Устанавливает строку подключения к файловой информационной базе
     */
    fun connectToFile(path: String) {
        this.connectionString = "File=\"$path\";"
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
     * Устанавливает путь к конфигурации запуска тестов
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
     * Добавляет дополнительные параметры
     */
    fun param(param: String) {
        additionalParams.add(param)
    }

    /**
     * Строит аргументы для запуска 1С:Предприятие
     */
    suspend fun buildBaseArgs(): List<String> {
        val args = mutableListOf<String>()

        // Путь к исполняемому файлу 1С:Предприятие
        val location = platformContext.locateUtility(io.github.alkoleft.mcp.core.modules.UtilityType.ENTERPRISE)
        args.add(location.executablePath.toString())
        args.add("ENTERPRISE")

        // Строка подключения
        if (connectionString.isNotEmpty()) {
            args.add("/IBConnectionString")
            args.add(connectionString)
        }

        // Пользователь
        user?.let { args.add("/N$it") }

        // Пароль
        password?.let { args.add("/P$it") }

        // Путь к конфигурации запуска тестов
        configPath?.let { args.add("/C") }

        // Путь для вывода
        outputPath?.let { args.add("/Out$it") }

        // Путь для лог-файла
        logPath?.let { args.add("/LogFile$it") }

        // Код языка
        language?.let { args.add("/L$it") }

        // Код локализации
        localization?.let { args.add("/VL$it") }

        // Отключение диалогов
        if (disableStartupDialogs) {
            args.add("/DisableStartupDialogs")
        }

        // Отключение сообщений
        if (disableStartupMessages) {
            args.add("/DisableStartupMessages")
        }

        // Дополнительные параметры
        args.addAll(additionalParams.toList())

        return args
    }

    /**
     * Строит аргументы для запуска тестов
     */
    suspend fun buildTestArgs(configPath: Path): List<String> {
        val args = buildBaseArgs().toMutableList()
        
        // Добавляем параметр запуска тестов
        args.add("/C")
        args.add("RunUnitTests=${configPath.toAbsolutePath()}")
        
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