package io.github.alkoleft.mcp.interfaces.cli.commands

import io.github.alkoleft.mcp.interfaces.cli.commands.test.RunAllCommand
import io.github.alkoleft.mcp.interfaces.cli.commands.test.RunListCommand
import io.github.alkoleft.mcp.interfaces.cli.commands.test.RunModuleCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import picocli.CommandLine.Command
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger { }

/**
 * Test command group with subcommands for different test execution modes.
 */
@Component
@Command(
    name = "test",
    description = ["Команды для выполнения тестов"],
    subcommands = [RunAllCommand::class, RunModuleCommand::class, RunListCommand::class],
    mixinStandardHelpOptions = true,
)
class TestCommand : Callable<Int> {
    override fun call(): Int {
        logger.info { "Use 'test run-all', 'test run-module', or 'test run-list' subcommands" }
        return 0
    }
}
