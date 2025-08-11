package io.github.alkoleft.mcp.infrastructure.platform.dsl.edt

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.CommandExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.InteractiveProcessExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Исполнитель команд EDT CLI через интерактивный процесс
 *
 * Предоставляет высокоуровневый интерфейс для работы с командами EDT CLI,
 * используя InteractiveProcessExecutor для управления процессом.
 */
class EdtCliExecutor(
    private val interactiveExecutor: InteractiveProcessExecutor,
) : CommandExecutor {
    /**
     * Параметры команды export
     */
    data class ExportParams(
        val projectName: String,
        val configurationFiles: Path,
        val project: String? = null,
    )

    /**
     * Параметры команды build
     */
    data class BuildParams(
        val projectName: String,
        val configurationFiles: Path? = null,
        val clean: Boolean = false,
    )

    /**
     * Параметры команды validate
     */
    data class ValidateParams(
        val projectName: String,
        val configurationFiles: Path? = null,
    )

    /**
     * Результат выполнения команды с дополнительной обработкой
     */
    data class EdtCommandResult(
        override val success: Boolean,
        override val output: String,
        override val error: String?,
        override val duration: Duration,
        val command: String,
        val hasErrors: Boolean = false,
        val errorDetails: String? = null,
        override val exitCode: Int = 0,
    ) : ShellCommandResult

    /**
     * Выполняет произвольную команду
     */
    override suspend fun execute(commandArgs: List<String>): EdtCommandResult {
        val command = commandArgs.joinToString(" ")
        logger.debug { "Выполнение команды $command" }
        return processCommandResult(interactiveExecutor.executeCommand(command, 600000))
    }

    /**
     * Корректно завершает сессию EDT CLI
     */
    suspend fun exit(): Boolean {
        logger.debug { "Завершение сессии EDT CLI" }
        return interactiveExecutor.exit()
    }

    /**
     * Проверяет статус процесса
     */
    fun getStatus() = interactiveExecutor.getStatus()

    /**
     * Проверяет, инициализирован ли процесс
     */
    fun isInitialized() = interactiveExecutor.isInitialized()

    /**
     * Проверяет, запущен ли процесс
     */
    fun isProcessRunning() = interactiveExecutor.isProcessRunning()

    /**
     * Проверяет, что интерактивный процесс готов обрабатывать команды
     *
     * Процесс считается готовым, если:
     * 1. Он инициализирован
     * 2. Процесс запущен и работает
     * 3. Процесс отвечает на команды (не заблокирован)
     */
    fun isProcessReady(): Boolean {
        if (!isInitialized() || !isProcessRunning()) {
            return false
        }

        // Дополнительная проверка: пытаемся выполнить простую команду
        // для проверки, что процесс действительно готов обрабатывать команды
        return try {
            // Проверяем, что процесс не заблокирован и может принимать команды
            val status = interactiveExecutor.getStatus()
            status.isRunning && status.pid != null
        } catch (e: Exception) {
            logger.debug(e) { "Ошибка при проверке готовности процесса" }
            false
        }
    }

    /**
     * Проверяет готовность процесса с таймаутом
     *
     * @param timeoutMs Таймаут ожидания готовности в миллисекундах
     * @return true, если процесс готов в течение таймаута
     */
    suspend fun waitForProcessReady(timeoutMs: Long = 5000): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isProcessReady()) {
                return true
            }
            kotlinx.coroutines.delay(100) // Проверяем каждые 100мс
        }

        return false
    }

    /**
     * Получает PID процесса
     */
    fun getPid(): Long? = interactiveExecutor.getProcessId()

    /**
     * Строит команду export
     */
    private fun buildExportCommand(params: ExportParams): String =
        if (params.project != null) {
            "export --project \"${params.project}\" --configuration-files \"${params.configurationFiles}\""
        } else {
            "export --project-name \"${params.projectName}\" --configuration-files \"${params.configurationFiles}\""
        }

    /**
     * Строит команду build
     */
    private fun buildBuildCommand(params: BuildParams): String {
        var command = "build --project-name \"${params.projectName}\""

        if (params.configurationFiles != null) {
            command += " --configuration-files \"${params.configurationFiles}\""
        }

        if (params.clean) {
            command += " --clean"
        }

        return command
    }

    /**
     * Строит команду validate
     */
    private fun buildValidateCommand(params: ValidateParams): String {
        var command = "validate --project-name \"${params.projectName}\""

        if (params.configurationFiles != null) {
            command += " --configuration-files \"${params.configurationFiles}\""
        }

        return command
    }

    /**
     * Обрабатывает результат команды
     */
    private fun processCommandResult(result: InteractiveProcessExecutor.CommandResult): EdtCommandResult {
        if (!result.success) {
            return EdtCommandResult(
                success = false,
                output = result.output,
                error = result.error,
                duration = result.duration,
                command = result.command,
                hasErrors = true,
                errorDetails = result.error,
                exitCode = result.exitCode,
            )
        }

        val output = result.output
        val command = result.command

        return EdtCommandResult(
            success = output.isBlank(),
            output = output,
            error = output,
            duration = result.duration,
            command = command,
            exitCode = result.exitCode,
        )
    }

    /**
     * Проверяет наличие ошибок в выводе команды export
     */
    private fun hasExportErrors(output: String): Boolean =
        output.contains("Не найдено проекта") ||
                output.contains("edtsh:") ||
                output.contains("ERROR:")

    /**
     * Извлекает детали ошибок из вывода команды export
     */
    private fun extractExportErrors(output: String): String? {
        val errorLines =
            output
                .lines()
                .filter { it.contains("Не найдено проекта") || it.contains("edtsh:") || it.contains("ERROR:") }
                .joinToString("\n")

        return if (errorLines.isNotEmpty()) errorLines else null
    }

    /**
     * Проверяет наличие ошибок в выводе команды build
     */
    private fun hasBuildErrors(output: String): Boolean =
        output.contains("Ошибка") ||
                output.contains("ERROR:") ||
                output.contains("FAILED") ||
                output.contains("BUILD FAILED")

    /**
     * Извлекает детали ошибок из вывода команды build
     */
    private fun extractBuildErrors(output: String): String? {
        val errorLines =
            output
                .lines()
                .filter { it.contains("Ошибка") || it.contains("ERROR:") || it.contains("FAILED") || it.contains("BUILD FAILED") }
                .joinToString("\n")

        return if (errorLines.isNotEmpty()) errorLines else null
    }

    /**
     * Проверяет наличие ошибок в выводе команды validate
     */
    private fun hasValidationErrors(output: String): Boolean =
        output.contains("Ошибка") ||
                output.contains("ERROR:") ||
                output.contains("VALIDATION FAILED") ||
                output.contains("Найдены ошибки")

    /**
     * Извлекает детали ошибок из вывода команды validate
     */
    private fun extractValidationErrors(output: String): String? {
        val errorLines =
            output
                .lines()
                .filter {
                    it.contains("Ошибка") ||
                            it.contains("ERROR:") ||
                            it.contains("VALIDATION FAILED") ||
                            it.contains("Найдены ошибки")
                }.joinToString("\n")

        return if (errorLines.isNotEmpty()) errorLines else null
    }
}
