/*
 * This file is part of METR.
 *
 * Copyright (C) 2025 Aleksey Koryakin <alkoleft@gmail.com> and contributors.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * METR is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * METR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with METR.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.alkoleft.mcp.infrastructure.platform.dsl.process

import io.github.alkoleft.mcp.application.core.ShellCommandResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.toDuration

private val logger = KotlinLogging.logger {}

/**
 * Исполнитель процессов для выполнения команд утилит платформы 1С
 *
 * Предоставляет асинхронное выполнение команд с захватом вывода
 * и обработкой ошибок.
 */
class ProcessExecutor : CommandExecutor {
    /**
     * Данные о потоках вывода процесса
     */
    private data class ProcessStreamData(
        val stdout: StringBuilder,
        val stderr: StringBuilder,
        val stdoutLineCount: Int,
        val stderrLineCount: Int,
    )

    /**
     * Параметры выполнения процесса
     */
    private data class ExecutionParams(
        val commandArgs: List<String>,
        val workingDirectory: Path? = null,
        val timeoutMs: Long? = null,
        val logFilePath: Path? = null,
        val includeStdout: Boolean = true,
    )

    /**
     * Читает потоки stdout и stderr процесса
     */
    private fun readProcessStreams(
        process: Process,
        includeStdout: Boolean = true,
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
                logger.debug { "STDOUT: $stdoutLine" }
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
            logger.debug { "STDERR: $stderrLine" }
            if (stderrLineCount % 50 == 0) {
                logger.debug { "Прочитано строк stderr: $stderrLineCount" }
            }
        }

        logger.debug { "Завершено чтение вывода. STDOUT: $stdoutLineCount строк, STDERR: $stderrLineCount строк" }

        return ProcessStreamData(stdout, stderr, stdoutLineCount, stderrLineCount)
    }

    /**
     * Создает объединенный вывод из различных источников
     */
    private fun buildCombinedOutput(
        streamData: ProcessStreamData,
        logContent: String = "",
        includeStdout: Boolean = true,
    ): String {
        val combinedOutput = StringBuilder()

        if (logContent.isNotEmpty()) {
            combinedOutput.append("=== ЛОГ ===\n").append(logContent)
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
     * Логирует результат выполнения процесса
     */
    private fun logProcessResult(
        process: Process,
        exitCode: Int,
        duration: Duration,
        streamData: ProcessStreamData,
        logContent: String = "",
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
            logger.warn { "============ ПРОЦЕСС ЗАВЕРШИЛСЯ С ОШИБКОЙ! ============" }
        } else {
//            if (streamData.stdout.isNotEmpty()) {
//                logger.info { "STDOUT вывод: ${streamData.stdout.toString().trim()}" }
//            }
//            if (streamData.stderr.isNotEmpty()) {
//                logger.warn { "STDERR вывод (предупреждения): ${streamData.stderr.toString().trim()}" }
//            }
            if (logContent.isNotBlank()) {
                logger.debug { "Логи 1С: $logContent" }
            }
            logger.debug { "============ ПРОЦЕСС ВЫПОЛНЕН УСПЕШНО ============" }
        }
    }

    /**
     * Создает результат выполнения процесса
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
     * Создает результат для ошибки выполнения
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
     * Базовый метод выполнения процесса с различными параметрами
     */
    private fun executeProcess(params: ExecutionParams): ProcessResult {
        val startTime = System.currentTimeMillis()
        val commandString = params.commandArgs.joinToString(" ")

        // Определяем фактический путь логирования
        val actualLogPath = params.logFilePath

        logStartProcess(params, actualLogPath, commandString)

        try {
            val processBuilder = createProcessBuilder(params.commandArgs, params.workingDirectory)
            val process = processBuilder.start()

            logger.debug { "Процесс запущен с PID: ${process.pid()}" }

            val result =
                if (params.timeoutMs != null) {
                    executeWithTimeoutLogic(process, params, startTime, actualLogPath)
                } else {
                    executeNormalLogic(process, params, startTime, actualLogPath)
                }

            return result
        } catch (e: Exception) {
            val duration = (System.currentTimeMillis() - startTime).toDuration(kotlin.time.DurationUnit.MILLISECONDS)
            return createErrorResult(e, duration, commandString, actualLogPath)
        }
    }

    /**
     * Логирует начало выполнения процесса
     */
    private fun logStartProcess(
        params: ExecutionParams,
        logPath: Path?,
        commandString: String,
    ) {
        logger.debug { "============ ЗАПУСК ПРОЦЕССА ============" }
        logger.debug { "Команда: $commandString" }

        if (logPath != null) {
            logger.debug { "Файл логов: $logPath" }
        }

        if (params.timeoutMs != null) {
            logger.debug { "Таймаут: ${params.timeoutMs}ms" }
        }

        if (params.workingDirectory != null) {
            logger.debug { "Рабочий каталог: ${params.workingDirectory}" }
            logger.debug { "Текущий каталог: ${System.getProperty("user.dir")}\n" }
        } else {
            logger.debug { "Рабочий каталог: ${System.getProperty("user.dir")}\n" }
        }
    }

    /**
     * Создает и настраивает ProcessBuilder
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
     * Выполняет процесс без таймаута
     */
    private fun executeNormalLogic(
        process: Process,
        params: ExecutionParams,
        startTime: Long,
        logPath: Path?,
    ): ProcessResult {
        val streamData = readProcessStreams(process, params.includeStdout)
        val exitCode = process.waitFor()
        val duration = (System.currentTimeMillis() - startTime).toDuration(kotlin.time.DurationUnit.MILLISECONDS)

        val logContent = if (logPath != null) readLogFile(logPath) else ""
        logProcessResult(process, exitCode, duration, streamData, logContent, params.workingDirectory)

        val combinedOutput = buildCombinedOutput(streamData, logContent, params.includeStdout)
        return createProcessResult(exitCode, duration, combinedOutput, logPath)
    }

    /**
     * Выполняет процесс с таймаутом
     */
    private fun executeWithTimeoutLogic(
        process: Process,
        params: ExecutionParams,
        startTime: Long,
        logPath: Path?,
    ): ProcessResult {
        val timeoutMs = params.timeoutMs!!

        // Запускаем процесс в отдельном потоке с таймаутом
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

        // Проверяем завершился ли поток за timeoutMs миллисекунд
        val finished = waitForProcessWithTimeout(processThread, timeoutMs)
        val duration = (System.currentTimeMillis() - startTime).toDuration(kotlin.time.DurationUnit.MILLISECONDS)

        if (!finished) {
            logger.warn { "Процесс превысил таймаут ${timeoutMs}ms, принудительное завершение" }
            process.destroyForcibly()
            processThread.interrupt()

            return createProcessResult(-1, duration, "", logPath)
        } else {
            logger.debug { "Процесс завершился в пределах таймаута, чтение вывода" }
            val streamData = readProcessStreams(process, params.includeStdout)
            val exitCode = process.exitValue()

            val logContent = if (logPath != null) readLogFile(logPath) else ""
            logProcessResult(process, exitCode, duration, streamData, logContent, params.workingDirectory)

            val combinedOutput = buildCombinedOutput(streamData, logContent, params.includeStdout)
            return createProcessResult(exitCode, duration, combinedOutput, logPath)
        }
    }

    /**
     * Ожидает завершения процесса с таймаутом
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
     * Выполняет команду с указанными аргументами
     */
    override fun execute(commandArgs: List<String>): ProcessResult =
        executeProcess(
            ExecutionParams(
                commandArgs = commandArgs,
            ),
        )

    /**
     * Выполняет команду с логированием
     *
     * @param commandArgs аргументы команды
     * @param logFilePath путь к файлу логов (если null, генерируется автоматически)
     * @param includeStdout включать ли вывод stdout в результат
     * @return результат выполнения с объединенным выводом из файла логов и stdout
     */
    fun executeWithLogging(
        commandArgs: List<String>,
        logFilePath: Path,
        includeStdout: Boolean = true,
    ): ProcessResult =
        executeProcess(
            ExecutionParams(
                commandArgs = commandArgs,
                logFilePath = logFilePath,
                includeStdout = includeStdout,
            ),
        )

    /**
     * Читает содержимое файла логов
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
 * Результат выполнения процесса
 */
data class ProcessResult(
    override val success: Boolean,
    override val output: String,
    override val error: String?,
    override val exitCode: Int,
    override val duration: Duration,
    val logFilePath: Path? = null,
) : ShellCommandResult
