package io.github.alkoleft.mcp

import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import io.github.alkoleft.mcp.interfaces.cli.BootstrapCli
import io.github.alkoleft.mcp.interfaces.cli.RunnerCli
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import picocli.CommandLine
import picocli.CommandLine.IFactory


@EnableConfigurationProperties
@SpringBootApplication
class McpYaxUnitRunnerApplication

fun main(args: Array<String>) {
    if (PlatformDetector.isWindows) {
        System.setProperty("file.encoding", "UTF-8")
    }
    val profile = BootstrapCli.detectProfile(args)
    runApplication<McpYaxUnitRunnerApplication>(*args) {
        setAdditionalProfiles(profile)
    }
}

@Profile("cli")
@Component
class ApplicationRunner(private val runner: RunnerCli, private val factory: IFactory) : CommandLineRunner,
    ExitCodeGenerator {
    private var exitCode = 0

    override fun run(vararg args: String) {
        exitCode = CommandLine(runner, factory).execute(*args)
    }

    override fun getExitCode(): Int {
        return exitCode
    }
}

@Primary
@Profile("cli")
@Component
class SpringPicocliFactory(private val applicationContext: ApplicationContext) : IFactory {
    override fun <K> create(cls: Class<K?>): K? {
        return applicationContext.getBean<K?>(cls)
    }
}