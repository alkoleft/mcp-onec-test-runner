package io.github.alkoleft.mcp.interfaces.cli

import io.github.alkoleft.mcp.McpYaxUnitRunnerApplication
import io.github.alkoleft.mcp.interfaces.cli.commands.McpCommand
import io.github.alkoleft.mcp.interfaces.cli.commands.TestCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger { }

/**
 * Main CLI entry point for MCP YaXUnit Runner.
 * Provides commands for running MCP server and executing tests.
 */
@org.springframework.context.annotation.Profile("cli")
@Component
@Command(
    name = "mcp-yaxunit-runner",
    description = ["MCP Server для работы с модульными тестами решений на платформе 1С:Предприятие"],
    subcommands = [McpCommand::class, TestCommand::class],
    mixinStandardHelpOptions = true,
)
class RunnerCli : Callable<Int> {
    @Option(
        names = ["--project"],
        description = ["Путь к проекту 1С:Предприятие (обязательно)"],
        required = false
    )
    var projectPath: String? = null

    @Option(
        names = ["--tests"],
        description = ["Путь к директории тестов (по умолчанию: ./tests)"],
        defaultValue = "./tests",
    )
    lateinit var testsPath: String

    @Option(
        names = ["--ib-conn"],
        description = ["Строка подключения к информационной базе (Srvr=...;Ref=...; или /F...)"],
        required = false,
    )
    var ibConnection: String? = null

    @Option(
        names = ["--ib-user"],
        description = ["Пользователь ИБ (переопределяет env IB_USER)"],
    )
    var ibUser: String? = null

    @Option(
        names = ["--ib-pwd"],
        description = ["Пароль ИБ (переопределяет env IB_PWD)"],
    )
    var ibPassword: String? = null

    @Option(
        names = ["--platform-ver"],
        description = ["Версия платформы 1С (8.3.24.1482 ...)"],
    )
    var platformVersion: String? = null

    @Option(
        names = ["--log-file"],
        description = ["Путь к файлу логов (опционально)"],
    )
    var logFile: String? = null

    @Option(
        names = ["--config"],
        description = ["Путь к файлу конфигурации YAML/JSON"],
    )

    private var applicationContext: ApplicationContext? = null

    fun setApplicationContext(context: ApplicationContext) {
        this.applicationContext = context
    }

    fun getApplicationContext(): ApplicationContext? = applicationContext

    override fun call(): Int {
        logger.info { "MCP YaXUnit Runner CLI initialized" }
        return 0
    }

    companion object {
        /**
         * Parse and execute CLI commands with Spring context
         */
        fun parseAndExecute(args: Array<String>): Int {
            return try {
                // Start Spring application context to get beans
                val app = SpringApplication(McpYaxUnitRunnerApplication::class.java)
                val context: ConfigurableApplicationContext = app.run(*args)

                // Get RunnerCli bean from Spring context
                val runnerCli = context.getBean(RunnerCli::class.java)

                // Set application context for commands
                runnerCli.setApplicationContext(context)

                val commandLine = CommandLine(runnerCli)

                // Configure command line options
                commandLine.isCaseInsensitiveEnumValuesAllowed = true
                commandLine.isAbbreviatedSubcommandsAllowed = true
                commandLine.isAbbreviatedOptionsAllowed = true

                val exitCode = commandLine.execute(*args)

                // Close Spring context
                context.close()

                exitCode
            } catch (e: Exception) {
                logger.error(e) { "CLI execution failed" }
                1
            }
        }
    }
}
