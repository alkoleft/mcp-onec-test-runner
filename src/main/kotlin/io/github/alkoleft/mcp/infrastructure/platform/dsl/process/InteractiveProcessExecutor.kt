package io.github.alkoleft.mcp.infrastructure.platform.dsl.process

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import kotlin.time.toDuration

private val logger = KotlinLogging.logger {}

/**
 * Интерактивный исполнитель процессов для работы с EDT CLI
 *
 * Предоставляет возможность отправлять команды в уже запущенный процесс
 * и получать ответы в реальном времени. Поддерживает интерактивные сессии
 * с автоматическим ожиданием приглашения командной строки.
 */
class InteractiveProcessExecutor(
    private var process: Process?,
    private val params: InteractiveProcessParams = InteractiveProcessParams(),
) {
    private var processStatus: ProcessStatus = ProcessStatus.PENDING
    private var processWriter: BufferedWriter? = null
    private var errorReader: BufferedReader? = null
    private val isRunning = AtomicBoolean(false)
    private val startTime = System.currentTimeMillis()

    // Определяем кодировку в зависимости от платформы
    private val consoleEncoding: String =
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            "CP866" // Windows console encoding
        } else {
            "UTF-8" // Linux/Mac
        }

    enum class ProcessStatus {
        PENDING,
        STARTED,
        STOPED,
        COMMAND_WAIT,
        COMMAND_EXECUTE,
    }

    /**
     * Параметры инициализации интерактивного процесса
     */
    data class InteractiveProcessParams(
        val promptPattern: String = "1C:EDT>",
        val promptTimeoutMs: Long = 180000, // 3 минуты по умолчанию для EDT CLI
        val commandTimeoutMs: Long = 60000, // 60 секунд по умолчанию для команд EDT
        val maxOutputLines: Int = 1000,
        val readDelayMs: Long = 50, // Задержка между чтениями
        val exitDelayMs: Long = 1000, // Задержка при выходе
    )

    companion object {
        private const val DEFAULT_EXIT_TIMEOUT = 5000L
        private const val DEFAULT_READ_DELAY = 50L
        private const val DEFAULT_EXIT_DELAY = 1000L
    }

    /**
     * Результат выполнения команды
     */
    data class CommandResult(
        override val success: Boolean,
        override val output: String,
        override val error: String?,
        override val exitCode: Int = 0,
        override val duration: Duration,
        val command: String,
    ) : ShellCommandResult

    /**
     * Статус процесса
     */
    data class ProcessState(
        val isRunning: Boolean,
        val pid: Long?,
        val uptime: Duration,
        val exitCode: Int?,
    )

    /**
     * Проверяет, что процесс активен и запущен
     */
    private fun isProcessActive(): Boolean = isRunning.get() && process?.isAlive == true

    val available
        get() = processStatus == ProcessStatus.COMMAND_WAIT

    /**
     * Инициализирует интерактивный процесс с уже запущенным процессом
     */
    suspend fun initialize(): Boolean =
        withContext(Dispatchers.IO) {
            processStatus = ProcessStatus.STARTED
            try {
                logger.debug { "Инициализация интерактивного процесса EDT CLI" }
                logger.debug { "Используется кодировка консоли: $consoleEncoding" }

                // Инициализируем потоки ввода-вывода
                isRunning.set(true)

                // Ждем появления приглашения командной строки
                logger.debug { "Ожидание инициализации приложения, ожидание приглашения." }
                val promptFound = waitForPrompt(params.promptTimeoutMs)
                if (!promptFound) {
                    logger.warn { "Приглашение командной строки не найдено в течение ${params.promptTimeoutMs}ms" }
                    stopProcess()
                    return@withContext false
                }

                logger.info { "Интерактивный процесс EDT CLI успешно инициализирован" }
                processStatus = ProcessStatus.COMMAND_WAIT
                true
            } catch (e: Exception) {
                logger.error(e) { "Ошибка при инициализации интерактивного процесса" }
                stopProcess()
                false
            }
        }

    /**
     * Читает данные из потока с проверкой prompt
     */
    private suspend fun readStreamData(timeoutMs: Long): String =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val output = StringBuilder()
            val byteBuffer = ByteArray(1024)

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                // Проверяем, не завершился ли процесс
                if (!isProcessActive()) {
                    logger.warn { "Процесс завершился во время чтения данных" }
                    break
                }

                val available = process!!.inputStream.available()
                if (available > 0) {
                    val readed = process!!.inputStream.read(byteBuffer)
                    output.append(String(byteBuffer, 0, readed, charset(consoleEncoding)))

                    // Проверяем prompt если нужно
                    if (output.contains(params.promptPattern)) {
                        val currentOutput = output.toString()
                        val promptCount = currentOutput.split(params.promptPattern).size - 1
                        logger.debug { "Найдено приглашений: $promptCount" }
                        logger.info { "Приглашение EDT: '${params.promptPattern}'" }
                        logger.info { "Последние 1000 символов вывода: ${currentOutput.takeLast(1000)}" }
                        return@withContext currentOutput
                    }
                }

                // Проверяем stderr на наличие ошибок
                while (process!!.errorStream.available() > 0) {
                    val char = process!!.errorStream.read().toChar()
                    output.append("ERROR: $char")
                    logger.debug { "STDERR: $char" }
                }

                kotlinx.coroutines.delay(params.readDelayMs)
            }

            throw TimeoutException()
        }

    /**
     * Ожидает появления приглашения командной строки
     */
    private suspend fun waitForPrompt(timeoutMs: Long): Boolean =
        withContext(Dispatchers.IO) {
            // Инициализируем потоки один раз
            processWriter = BufferedWriter(OutputStreamWriter(process!!.outputStream, consoleEncoding))
            errorReader = BufferedReader(InputStreamReader(process!!.errorStream, consoleEncoding))

            try {
                readStreamData(timeoutMs)
                return@withContext true
            } catch (_: TimeoutException) {
                logger.warn { "Таймаут ожидания приглашения." }
                false
            }
        }

    /**
     * Выполняет команду в интерактивном процессе
     */
    suspend fun executeCommand(
        command: String,
        timeoutMs: Long? = null,
    ): CommandResult =
        withContext(Dispatchers.IO) {
            if (!isProcessActive()) {
                return@withContext CommandResult(
                    success = false,
                    output = "",
                    error = "Процесс не запущен или завершен",
                    duration = (System.currentTimeMillis() - startTime).toDuration(DurationUnit.MILLISECONDS),
                    command = command,
                )
            }

            val commandStartTime = System.currentTimeMillis()
            val actualTimeout = timeoutMs ?: params.commandTimeoutMs

            try {
                sendCommand(command)

                // Читаем ответ до следующего приглашения
                val (output, duration) = measureTimedValue { readStreamData(actualTimeout) }

                CommandResult(
                    success = true,
                    output = output.replace(params.promptPattern, "").trim(),
                    error = null,
                    duration = duration,
                    command = command,
                )
            } catch (e: Exception) {
                val duration = (System.currentTimeMillis() - commandStartTime).toDuration(DurationUnit.MILLISECONDS)
                logger.error(e) { "Ошибка при выполнении команды: $command" }

                CommandResult(
                    success = false,
                    output = "",
                    error = e.message ?: "Unknown error",
                    duration = duration,
                    command = command,
                )
            }
        }

    /**
     * Отправляет команду exit для корректного завершения
     */
    suspend fun exit(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                logger.debug { "Отправка команды exit для корректного завершения" }
                executeCommand("exit --yes", DEFAULT_EXIT_TIMEOUT)

                // Даем процессу время на корректное завершение
                kotlinx.coroutines.delay(params.exitDelayMs)

                stopProcess()
                true
            } catch (e: Exception) {
                logger.error(e) { "Ошибка при отправке команды exit" }
                stopProcess()
                false
            }
        }

    /**
     * Останавливает процесс принудительно
     */
    fun stopProcess() {
        try {
            logger.debug { "Остановка интерактивного процесса" }

            isRunning.set(false)
            processStatus = ProcessStatus.STOPED
            // Закрываем потоки
            processWriter?.close()
            errorReader?.close()

            // Завершаем процесс
            process?.let { proc ->
                if (proc.isAlive) {
                    proc.destroyForcibly()
                    logger.debug { "Процесс принудительно завершен" }
                }
            }

            process = null
            processWriter = null
            errorReader = null

            logger.debug { "Интерактивный процесс остановлен" }
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при остановке процесса" }
        }
    }

    /**
     * Получает текущий статус процесса
     */
    fun getStatus(): ProcessState {
        val currentProcess = process
        return ProcessState(
            isRunning = isProcessActive(),
            pid = currentProcess?.pid(),
            uptime = (System.currentTimeMillis() - startTime).toDuration(DurationUnit.MILLISECONDS),
            exitCode = if (currentProcess?.isAlive == true) null else currentProcess?.exitValue(),
        )
    }

    /**
     * Проверяет, инициализирован ли процесс
     */
    fun isInitialized(): Boolean = isProcessActive()

    /**
     * Проверяет, запущен ли процесс
     */
    fun isProcessRunning(): Boolean = isProcessActive()

    /**
     * Получает PID процесса
     */
    fun getProcessId(): Long? = process?.pid()

    private fun sendCommand(command: String) {
        logger.debug { "Выполнение команды: $command" }

        // Отправляем команду
        processWriter?.write(command)
        processWriter?.newLine()
        processWriter?.flush()
    }
}
