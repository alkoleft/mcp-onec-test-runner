package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import io.github.alkoleft.mcp.core.modules.ShellCommandResult

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
    protected abstract suspend fun executeCommand(command: C): ShellCommandResult


    /**
     * Строит аргументы команды для конфигуратора с произвольными аргументами
     *
     * @param commandArgs аргументы команды
     * @return полный список аргументов для выполнения
     */
    protected fun buildCommandArgsWithArgs(commandArgs: List<String>): List<String> {
        val args = mutableListOf<String>()

        // Базовые аргументы конфигуратора
        args.addAll(context.buildBaseArgs())

        // Команда и её аргументы (команда уже включена в commandArgs)
        args.addAll(commandArgs)

        return args
    }
}