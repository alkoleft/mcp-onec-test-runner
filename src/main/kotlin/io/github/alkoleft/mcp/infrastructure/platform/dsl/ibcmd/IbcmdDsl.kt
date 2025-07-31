package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.executor.ProcessExecutor
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * DSL для работы с ibcmd (Инфобазный менеджер) 1С:Предприятие
 *
 * Предоставляет удобный интерфейс для выполнения операций с информационными базами
 * через fluent API и DSL синтаксис.
 */
class IbcmdDsl(
    private val context: PlatformUtilityContext
) {
    private var connectionString: String = ""
    private var user: String? = null
    private var password: String? = null
    private var outputPath: Path? = null
    private var logPath: Path? = null
    private var additionalParams: MutableList<String> = mutableListOf()

    /**
     * Устанавливает строку подключения к информационной базе
     */
    fun connect(connectionString: String) {
        this.connectionString = connectionString
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
     * Добавляет дополнительные параметры
     */
    fun param(param: String) {
        additionalParams.add(param)
    }

    /**
     * Выполняет создание информационной базы
     */
    fun create(): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand("CREATEINFOBASE")
        }
    }

    /**
     * Выполняет удаление информационной базы
     */
    fun drop(): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand("DROPINFOBASE")
        }
    }

    /**
     * Выполняет копирование информационной базы
     */
    fun copy(): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand("COPYINFOBASE")
        }
    }

    /**
     * Выполняет восстановление информационной базы
     */
    fun restore(): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand("RESTOREINFOBASE")
        }
    }

    /**
     * Выполняет сжатие информационной базы
     */
    fun compress(): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand("COMPRESSINFOBASE")
        }
    }

    /**
     * Выполняет реиндексацию информационной базы
     */
    fun reindex(): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand("REINDEXINFOBASE")
        }
    }

    /**
     * Выполняет проверку информационной базы
     */
    fun check(): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand("CHECKINFOBASE")
        }
    }

    /**
     * Выполняет обновление информационной базы
     */
    fun update(): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand("UPDATEINFOBASE")
        }
    }

    /**
     * Выполняет получение списка информационных баз
     */
    fun list(): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand("LISTINFOBASES")
        }
    }

    /**
     * Выполняет получение информации об информационной базе
     */
    fun info(): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand("INFOBASE")
        }
    }

    /**
     * Выполняет произвольную команду ibcmd
     */
    fun command(command: String): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand(command)
        }
    }

    /**
     * Выполняет команду ibcmd с синхронным API
     */
    fun commandSync(command: String): IbcmdResult {
        return runBlocking {
            executeIbcmdCommand(command)
        }
    }

    /**
     * Строит результат выполнения операций
     */
    fun buildResult(): IbcmdResult {
        return IbcmdResult(
            success = true,
            output = "",
            error = null,
            exitCode = 0,
            duration = Duration.ZERO
        )
    }

    /**
     * Выполняет команду ibcmd
     */
    private suspend fun executeIbcmdCommand(command: String): IbcmdResult {
        val duration = measureTime {
            try {
                val location = context.locateUtility(UtilityType.INFOBASE_MANAGER_IBCMD)
                val executor = ProcessExecutor()

                val commandArgs = buildCommandArgs(command, location.executablePath)
                val result = executor.execute(commandArgs)

                context.setResult(
                    success = result.exitCode == 0,
                    output = result.output,
                    error = result.error,
                    exitCode = result.exitCode,
                    duration = result.duration
                )

            } catch (e: Exception) {
                context.setResult(
                    success = false,
                    output = "",
                    error = e.message ?: "Unknown error",
                    exitCode = -1,
                    duration = Duration.ZERO
                )
            }
        }

        return IbcmdResult(
            success = context.buildResult().success,
            output = context.buildResult().output,
            error = context.buildResult().error,
            exitCode = context.buildResult().exitCode,
            duration = duration
        )
    }

    /**
     * Строит аргументы команды для ibcmd
     */
    private fun buildCommandArgs(command: String, executablePath: Path): List<String> {
        val args = mutableListOf<String>()

        // Путь к исполняемому файлу
        args.add(executablePath.toString())

        // Команда
        args.add(command)

        // Строка подключения
        if (connectionString.isNotEmpty()) {
            args.add(connectionString)
        }

        // Пользователь
        user?.let { args.add("/N$it") }

        // Пароль
        password?.let { args.add("/P$it") }

        // Путь для вывода
        outputPath?.let { args.add("/Out$it") }

        // Путь для лог-файла
        logPath?.let { args.add("/LogFile$it") }

        // Дополнительные параметры
        args.addAll(additionalParams)

        return args
    }
}

/**
 * Результат выполнения операций с ibcmd
 */
data class IbcmdResult(
    val success: Boolean,
    val output: String,
    val error: String?,
    val exitCode: Int,
    val duration: Duration
) 