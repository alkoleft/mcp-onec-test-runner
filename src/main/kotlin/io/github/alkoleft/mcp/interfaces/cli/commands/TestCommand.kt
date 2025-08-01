package io.github.alkoleft.mcp.interfaces.cli.commands

import io.github.alkoleft.mcp.interfaces.cli.commands.test.RunAllCommand
import io.github.alkoleft.mcp.interfaces.cli.commands.test.RunListCommand
import io.github.alkoleft.mcp.interfaces.cli.commands.test.RunModuleCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import picocli.CommandLine.Command
import picocli.CommandLine.ParentCommand
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger { }

/**
 * Test command group with subcommands for different test execution modes.
 */
@Command(
    name = "test",
    description = ["Команды для выполнения тестов"],
    subcommands = [RunAllCommand::class, RunModuleCommand::class, RunListCommand::class],
    mixinStandardHelpOptions = true,
)
class TestCommand : Callable<Int> {
    @ParentCommand
    private lateinit var parent: io.github.alkoleft.mcp.interfaces.cli.RunnerCli

    override fun call(): Int {
        logger.info { "Use 'test run-all', 'test run-module', or 'test run-list' subcommands" }
        return 0
    }
}
