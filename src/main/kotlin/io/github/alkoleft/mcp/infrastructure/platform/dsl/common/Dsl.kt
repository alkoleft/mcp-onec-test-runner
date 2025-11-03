package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Базовый DSL класс для выполнения команд
 *
 * @param T тип контекста DSL
 * @param C тип команды
 * @param context контекст DSL
 */
abstract class Dsl<T : DslContext, C : Command>(
    protected val context: T,
) {
    /**
     * Выполняет команду
     *
     * @param command команда для выполнения
     * @return результат выполнения команды
     */
    protected open fun executeCommand(
        command: C,
        logPath: Path? = null,
    ): ShellCommandResult {
        val executor = ProcessExecutor()
        val args = buildCommandArgs(command, logPath)

        logger.info { command.getFullDescription() }

        return if (logPath != null) {
            executor.executeWithLogging(args, logPath)
        } else {
            executor.execute(args)
        }
    }

    /**
     * Строит аргументы команды для конфигуратора с произвольными аргументами
     *
     * @param commandArgs аргументы команды
     * @return полный список аргументов для выполнения
     */
    protected open fun buildCommandArgs(
        command: C,
        logPath: Path? = null,
    ): List<String> {
        if (logPath != null) {
            throw IllegalArgumentException("Не поддерживается работы с файлом лога")
        }
        val args = mutableListOf<String>()

        // Базовые аргументы
        args.addAll(context.buildBaseArgs())

        // Команда и её аргументы (команда уже включена в commandArgs)
        args.addAll(command.arguments)

        return args
    }

    /**
     * Генерирует путь к файлу логов с временной меткой
     */
    protected fun generateLogFilePath(): Path = File.createTempFile("onec_log_", ".log").toPath()
}
