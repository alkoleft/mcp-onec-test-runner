package io.github.alkoleft.mcp.interfaces.cli

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.HelpCommand
import java.util.concurrent.Callable

@Command(
    name = "mcp-yaxunit-runner",
    description = ["Bootstrap CLI to select profile before starting Spring"],
    subcommands = [
        HelpCommand::class,
        BootstrapCli.Mcp::class,
        BootstrapCli.Test::class
    ],
    mixinStandardHelpOptions = true
)
class BootstrapCli : Callable<Int> {
    override fun call(): Int = 0

    @Command(
        name = "mcp",
        description = ["Start MCP server mode"],
        mixinStandardHelpOptions = true
    )
    class Mcp : Callable<Int> {
        override fun call(): Int = 0
    }

    @Command(
        name = "test",
        description = ["Run tests"],
        subcommands = [
            Test.RunAll::class,
            Test.RunModule::class,
            Test.RunList::class
        ],
        mixinStandardHelpOptions = true
    )
    class Test : Callable<Int> {
        override fun call(): Int = 0

        @Command(
            name = "run-all",
            description = ["Run all tests"],
            mixinStandardHelpOptions = true
        )
        class RunAll : Callable<Int> {
            override fun call(): Int = 0
        }

        @Command(
            name = "run-module",
            description = ["Run tests for a single module"],
            mixinStandardHelpOptions = true
        )
        class RunModule : Callable<Int> {
            override fun call(): Int = 0
        }

        @Command(
            name = "run-list",
            description = ["Run specific tests list"],
            mixinStandardHelpOptions = true
        )
        class RunList : Callable<Int> {
            override fun call(): Int = 0
        }
    }

    companion object {
        fun detectProfile(args: Array<String>): String {
            // Default profile
            var profile = "cli"
            val bootstrap = BootstrapCli()
            val cmd = CommandLine(bootstrap)
            val parseResult = try {
                cmd.parseArgs(*args)
            } catch (e: Exception) {
                return profile
            }
            val hasMcp = parseResult.hasSubcommand()
                    && parseResult.subcommand().commandSpec().name() == "mcp"
            if (hasMcp) profile = "mcp" else profile = "cli"
            return profile
        }
    }
}
