package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor

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
    protected fun executeCommand(command: C): ShellCommandResult {
        val executor = ProcessExecutor()
        val args = buildCommandArgs(command.arguments)

        return executor.executeWithLogging(args)
    }

    /**
     * Строит аргументы команды для конфигуратора с произвольными аргументами
     *
     * @param commandArgs аргументы команды
     * @return полный список аргументов для выполнения
     */
    protected fun buildCommandArgs(commandArgs: List<String>): List<String> {
        val args = mutableListOf<String>()

        // Базовые аргументы
        args.addAll(context.buildBaseArgs())

        // Команда и её аргументы (команда уже включена в commandArgs)
        args.addAll(commandArgs)

        return args
    }
}