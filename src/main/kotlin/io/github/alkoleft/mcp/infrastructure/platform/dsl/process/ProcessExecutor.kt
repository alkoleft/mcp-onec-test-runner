package io.github.alkoleft.mcp.infrastructure.platform.dsl.process

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.toDuration

private val logger = KotlinLogging.logger {}

/**
 * Исполнитель процессов для выполнения команд утилит платформы 1С.
 *
 * Предоставляет асинхронное выполнение команд с захватом вывода
 * и обработкой ошибок. Поддерживает логирование 1С через параметр /Out.
 */
class ProcessExecutor : CommandExecutor {
    /**
     * Данные о потоках вывода процесса.
     */
    private data class ProcessStreamData(
        val stdout: StringBuilder,
        val stderr: StringBuilder,
        val stdoutLineCount: Int,
        val stderrLineCount: Int,
    )

    /**
     * Параметры выполнения процесса.
     */
    private data class ExecutionParams(
        val commandArgs: List<String>,
        val workingDirectory: Path? = null,
        val timeoutMs: Long? = null,
        val logFilePath: Path? = null,
        val includeStdout: Boolean = true,
        val verboseLogging: Boolean = false,
        val processType: String = "процесс",
    )

    /**
     * Читает потоки stdout и stderr процесса.
     * Собирает вывод и подсчитывает строки для логирования.
     *
     * @param process Процесс для чтения потоков.
     * @param includeStdout Включать ли stdout.
     * @param verboseLogging Детальное логирование строк.
     * @return Данные о потоках.
     */
    private fun readProcessStreams(
        process: Process,
        includeStdout: Boolean = true,
        verboseLogging: Boolean = false,
    ): ProcessStreamData {
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        var stdoutLineCount = 0
        var stderrLineCount = 0

        if (includeStdout) {
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream, "UTF-8"))
            var stdoutLine: String?
            while (stdoutReader.readLine().also { stdoutLine = it } != null) {
                stdout.append(stdoutLine).append("\n")
                stdoutLineCount++
                if (verboseLogging) {
                    logger.debug { "STDOUT: $stdoutLine" }
                }
                if (stdoutLineCount % 50 == 0) {
                    logger.debug { "Прочитано строк stdout: $stdoutLineCount" }
                }
            }
        }

        val stderrReader = BufferedReader(InputStreamReader(process.errorStream, "UTF-8"))
        var stderrLine: String?
        while (stderrReader.readLine().also { stderrLine = it } != null) {
            stderr.append(stderrLine).append("\n")
            stderrLineCount++
            if (verboseLogging) {
                logger.debug { "STDERR: $stderrLine" }
            }
            if (stderrLineCount % 50 == 0) {
                logger.debug { "Прочитано строк stderr: $stderrLineCount" }
            }
        }

        logger.debug { "Завершено чтение вывода. STDOUT: $stdoutLineCount строк, STDERR: $stderrLineCount строк" }

        return ProcessStreamData(stdout, stderr, stdoutLineCount, stderrLineCount)
    }

    /**
     * Создает объединенный вывод из различных источников.
     * Включает логи 1С, stdout и stderr в формате с разделителями.
     *
     * @param streamData Данные потоков.
     * @param logContent Содержимое файла логов.
     * @param includeStdout Включать ли stdout.
     * @return Объединенный вывод как строка.
     */
    private fun buildCombinedOutput(
        streamData: ProcessStreamData,
        logContent: String = "",
        includeStdout: Boolean = true,
    ): String {
        val combinedOutput = StringBuilder()

        if (logContent.isNotEmpty()) {
            combinedOutput.append("=== ЛОГИ 1С ===\n").append(logContent)
        }

        if (includeStdout && streamData.stdout.isNotEmpty()) {
            combinedOutput.append("=== STDOUT ===\n").append(streamData.stdout)
        }

        if (streamData.stderr.isNotEmpty()) {
            combinedOutput.append("=== STDERR ===\n").append(streamData.stderr)
        }

        return combinedOutput.toString()
    }

    /**
     * Логирует результат выполнения процесса.
     * Выводит детали завершения, код выхода и выводы при ошибках.
     *
     * @param process Процесс.
     * @param exitCode Код выхода.
     * @param duration Время выполнения.
     * @param streamData Данные потоков.
     * @param logContent Содержимое логов.
     * @param processType Тип процесса.
     * @param workingDirectory Рабочий каталог.
     */
    private fun logProcessResult(
        process: Process,
        exitCode: Int,
        duration: Duration,
        streamData: ProcessStreamData,
        logContent: String = "",
        processType: String = "процесс",
        workingDirectory: Path? = null,
    ) {
        logger.debug {
            "Процесс завершен: PID: ${process.pid()}, Код завершения: $exitCode, Время выполнения: ${duration.inWholeMilliseconds}ms"
        }

        workingDirectory?.let { logger.debug { "Рабочий каталог: $workingDirectory" } }

        if (exitCode != 0) {
            logger.warn { "Код ошибки: $exitCode" }
            if (streamData.stderr.isNotEmpty()) {
                logger.warn { "STDERR вывод: ${streamData.stderr.toString().trim()}" }
            }
            if (streamData.stdout.isNotEmpty()) {
                logger.info { "STDOUT вывод: ${streamData.stdout.toString().trim()}" }
            }
            if (logContent.isNotBlank()) {
                logger.info { "Логи 1С: $logContent" }
            }
            logger.warn { "=== ${processType.uppercase()} ЗАВЕРШИЛСЯ С ОШИБКОЙ! ===" }
        } else {
            if (logContent.isNotBlank()) {
                logger.debug { "Логи 1С: $logContent" }
            }
            logger.debug { "=== ${processType.uppercase()} ВЫПОЛНЕН УСПЕШНО ===" }
        }
    }

    /**
     * Создает результат выполнения процесса.
     *
     * @param exitCode Код выхода.
     * @param duration Время выполнения.
     * @param combinedOutput Объединенный вывод.
     * @param logFilePath Путь к файлу логов.
     * @return ProcessResult.
     */
    private fun createProcessResult(
        exitCode: Int,
        duration: Duration,
        combinedOutput: String,
        logFilePath: Path? = null,
    ): ProcessResult =
        ProcessResult(
            success = exitCode == 0,
            output = combinedOutput,
            error = if (exitCode != 0) "Процесс завершился с кодом $exitCode" else null,
            exitCode = exitCode,
            duration = duration,
            logFilePath = logFilePath,
        )

    /**
     * Создает результат для ошибки выполнения.
     *
     * @param exception Исключение.
     * @param duration Время выполнения.
     * @param command Команда.
     * @param logFilePath Путь к файлу логов.
     * @return ProcessResult с ошибкой.
     */
    private fun createErrorResult(
        exception: Exception,
        duration: Duration,
        command: String,
        logFilePath: Path? = null,
    ): ProcessResult {
        logger.error(exception) { "Ошибка при выполнении процесса: $command" }
        return ProcessResult(
            success = false,
            output = "",
            error = exception.message ?: "Неизвестная ошибка",
            exitCode = -1,
            duration = duration,
            logFilePath = logFilePath,
        )
    }

    /**
     * Базовый метод выполнения процесса с различными параметрами.
     * Обрабатывает таймауты, логирование и очистку.
     *
     * @param params Параметры выполнения.
     * @return ProcessResult.
     */
    private suspend fun executeProcess(params: ExecutionParams): ProcessResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val commandString = params.commandArgs.joinToString(" ")

            val actualLogPath = params.logFilePath

            logStartProcess(params, actualLogPath, commandString)

            try {
                val enhancedArgs = prepareCommandArgs(params.commandArgs, actualLogPath)
                val processBuilder = createProcessBuilder(enhancedArgs, params.workingDirectory)
                val process = processBuilder.start()

                logger.debug { "Процесс запущен с PID: ${process.pid()}" }

                val result =
                    if (params.timeoutMs != null) {
                        executeWithTimeoutLogic(process, params, startTime, actualLogPath, params.verboseLogging)
                    } else {
                        executeNormalLogic(process, params, startTime, actualLogPath, params.verboseLogging)
                    }

                result
            } catch (e: Exception) {
                val duration = (System.currentTimeMillis() - startTime).toDuration(kotlin.time.DurationUnit.MILLISECONDS)
                createErrorResult(e, duration, commandString, actualLogPath)
            } finally {
                params.logFilePath?.let { logPath ->
                    try {
                        Files.deleteIfExists(logPath)
                        logger.debug { "Файл логов автоматически очищен: $logPath" }
                    } catch (e: Exception) {
                        logger.warn { "Не удалось автоматически очистить файл логов $logPath: ${e.message}" }
                    }
                }
            }
        }

    /**
     * Логирует начало выполнения процесса.
     *
     * @param params Параметры.
     * @param logPath Путь к логам.
     * @param commandString Строка команды.
     */
    private fun logStartProcess(
        params: ExecutionParams,
        logPath: Path?,
        commandString: String,
    ) {
        logger.debug { "=== ЗАПУСК ${params.processType.uppercase()} ===" }
        logger.debug { "Команда: $commandString${if (logPath != null) " /Out $logPath" else ""}" }

        if (logPath != null) {
            logger.debug { "Файл логов: $logPath" }
        }

        if (params.timeoutMs != null) {
            logger.debug { "Таймаут: ${params.timeoutMs}ms" }
        }

        if (params.workingDirectory != null) {
            logger.debug { "Рабочий каталог: ${params.workingDirectory}" }
            logger.debug { "Текущий каталог: ${System.getProperty("user.dir")}" }
        } else {
            logger.debug { "Рабочий каталог: ${System.getProperty("user.dir")}" }
        }
    }

    /**
     * Подготавливает аргументы команды, добавляя параметр /Out при необходимости.
     *
     * @param commandArgs Аргументы команды.
     * @param logPath Путь к логам.
     * @return Улучшенные аргументы.
     */
    private fun prepareCommandArgs(
        commandArgs: List<String>,
        logPath: Path?,
    ): List<String> =
        if (logPath != null && !commandArgs.any { it.startsWith("/Out") }) {
            commandArgs + "/Out" + logPath.toString()
        } else {
            commandArgs
        }

    /**
     * Создает и настраивает ProcessBuilder.
     *
     * @param commandArgs Аргументы.
     * @param workingDirectory Рабочий каталог.
     * @return ProcessBuilder.
     */
    private fun createProcessBuilder(
        commandArgs: List<String>,
        workingDirectory: Path?,
    ): ProcessBuilder {
        val processBuilder = ProcessBuilder(commandArgs)
        processBuilder.redirectErrorStream(false)

        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory.toFile())
        }

        return processBuilder
    }

    /**
     * Выполняет процесс без таймаута.
     *
     * @param process Процесс.
     * @param params Параметры.
     * @param startTime Время начала.
     * @param logPath Путь к логам.
     * @param verboseLogging Детальное логирование.
     * @return ProcessResult.
     */
    private fun executeNormalLogic(
        process: Process,
        params: ExecutionParams,
        startTime: Long,
        logPath: Path?,
        verboseLogging: Boolean = false,
    ): ProcessResult {
        val streamData = readProcessStreams(process, params.includeStdout, verboseLogging)
        val exitCode = process.waitFor()
        val duration = (System.currentTimeMillis() - startTime).toDuration(kotlin.time.DurationUnit.MILLISECONDS)

        val logContent = if (logPath != null) readLogFile(logPath) else ""
        logProcessResult(process, exitCode, duration, streamData, logContent, params.processType, params.workingDirectory)

        val combinedOutput = buildCombinedOutput(streamData, logContent, params.includeStdout)
        return createProcessResult(exitCode, duration, combinedOutput, logPath)
    }

    /**
     * Выполняет процесс с таймаутом.
     * Использует отдельный поток для мониторинга и принудительное завершение при превышении.
     *
     * @param process Процесс.
     * @param params Параметры.
     * @param startTime Время начала.
     * @param logPath Путь к логам.
     * @param verboseLogging Детальное логирование.
     * @return ProcessResult.
     */
    private fun executeWithTimeoutLogic(
        process: Process,
        params: ExecutionParams,
        startTime: Long,
        logPath: Path?,
        verboseLogging: Boolean = false,
    ): ProcessResult {
        val timeoutMs = params.timeoutMs!!

        val processThread =
            Thread {
                try {
                    logger.debug { "Ожидание завершения процесса в отдельном потоке" }
                    process.waitFor()
                    logger.debug { "Процесс завершился в отдельном потоке" }
                } catch (_: InterruptedException) {
                    logger.debug { "Поток процесса прерван, принудительное завершение" }
                    process.destroyForcibly()
                }
            }

        processThread.start()
        logger.debug { "Поток мониторинга процесса запущен" }

        val finished = waitForProcessWithTimeout(processThread, timeoutMs)
        val duration = (System.currentTimeMillis() - startTime).toDuration(kotlin.time.DurationUnit.MILLISECONDS)

        if (!finished) {
            logger.warn { "Процесс превысил таймаут ${params.timeoutMs}ms, принудительное завершение с логированием прерывания" }
            process.destroyForcibly()
            processThread.interrupt()
            logger.info { "Процесс принудительно завершен по таймауту, возможны остаточные ресурсы" }
            return createProcessResult(-1, duration, "", logPath)
        } else {
            logger.debug { "Процесс завершился в пределах таймаута, чтение вывода" }
            val streamData = readProcessStreams(process, params.includeStdout, verboseLogging)
            val exitCode = process.exitValue()

            val logContent = if (logPath != null) readLogFile(logPath) else ""
            logProcessResult(process, exitCode, duration, streamData, logContent, params.processType, params.workingDirectory)

            val combinedOutput = buildCombinedOutput(streamData, logContent, params.includeStdout)
            return createProcessResult(exitCode, duration, combinedOutput, logPath)
        }
    }

    /**
     * Ожидает завершения процесса с таймаутом.
     * Проверяет статус потока с интервалами.
     *
     * @param processThread Поток процесса.
     * @param timeoutMs Таймаут в мс.
     * @return true, если завершился вовремя.
     */
    private fun waitForProcessWithTimeout(
        processThread: Thread,
        timeoutMs: Long,
    ): Boolean {
        var finished = false
        val startJoin = System.currentTimeMillis()
        var checkCount = 0

        while (System.currentTimeMillis() - startJoin < timeoutMs) {
            if (!processThread.isAlive) {
                finished = true
                break
            }
            Thread.sleep(50)
            checkCount++
            if (checkCount % 100 == 0) {
                val elapsed = System.currentTimeMillis() - startJoin
                logger.debug { "Проверка процесса... Прошло: ${elapsed}ms из ${timeoutMs}ms" }
            }
        }

        return finished
    }

    /**
     * Выполняет команду с указанными аргументами.
     *
     * @param commandArgs Аргументы команды.
     * @return ProcessResult.
     */
    override suspend fun execute(commandArgs: List<String>): ProcessResult =
        executeProcess(
            ExecutionParams(
                commandArgs = commandArgs,
                processType = "процесс",
            ),
        )

    /**
     * Выполняет команду 1С с логированием в файл через параметр /Out.
     *
     * @param commandArgs Аргументы команды.
     * @param logFilePath Путь к файлу логов (если null, генерируется автоматически).
     * @param includeStdout Включать ли вывод stdout в результат.
     * @param verboseLogging Детальное логирование потоков.
     * @param autoCleanupLog Авто-очистка лог-файла после выполнения.
     * @return Результат выполнения с объединенным выводом из файла логов и stdout.
     */
    suspend fun executeWithLogging(
        commandArgs: List<String>,
        logFilePath: Path? = null,
        includeStdout: Boolean = true,
        verboseLogging: Boolean = false,
        autoCleanupLog: Boolean = true,
    ): ProcessResult {
        val effectiveLogPath = if (autoCleanupLog) logFilePath ?: generateLogFilePath() else logFilePath
        return executeProcess(
            ExecutionParams(
                commandArgs = commandArgs,
                logFilePath = effectiveLogPath,
                includeStdout = includeStdout,
                verboseLogging = verboseLogging,
                processType = "процесс 1С",
            ),
        )
    }

    /**
     * Выполняет команду с таймаутом.
     *
     * @param commandArgs Аргументы команды.
     * @param timeoutMs Таймаут в мс (по умолчанию 5 минут).
     * @return ProcessResult.
     */
    suspend fun executeWithTimeout(
        commandArgs: List<String>,
        timeoutMs: Long = 300000, // 5 минут по умолчанию
    ): ProcessResult =
        executeProcess(
            ExecutionParams(
                commandArgs = commandArgs,
                timeoutMs = timeoutMs,
                processType = "процесс с таймаутом",
            ),
        )

    /**
     * Генерирует путь к файлу логов с временной меткой.
     *
     * @return Path к временному файлу логов.
     */
    private fun generateLogFilePath(): Path = File.createTempFile("onec_log_", "log").toPath()

    /**
     * Читает содержимое файла логов.
     *
     * @param logPath Путь к файлу.
     * @return Содержимое файла или пустая строка при ошибке.
     */
    private fun readLogFile(logPath: Path): String =
        try {
            if (Files.exists(logPath)) {
                val content = Files.readString(logPath)
                logger.debug { "Успешно прочитан файл логов: $logPath (${content.length} символов)" }
                content
            } else {
                logger.warn { "Файл логов не найден: $logPath" }
                ""
            }
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при чтении файла логов: $logPath" }
            ""
        }
}

/**
 * Результат выполнения процесса.
 */
data class ProcessResult(
    override val success: Boolean,
    override val output: String,
    override val error: String?,
    override val exitCode: Int,
    override val duration: Duration,
    val logFilePath: Path? = null,
) : ShellCommandResult
