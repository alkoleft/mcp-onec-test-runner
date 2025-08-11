package io.github.alkoleft.mcp

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableConfigurationProperties(ApplicationProperties::class)
@EnableAsync
@SpringBootApplication
class McpYaxUnitRunnerApplication

fun main(args: Array<String>) {
    if (PlatformDetector.isWindows) {
        System.setProperty("file.encoding", "UTF-8")
    }

    runApplication<McpYaxUnitRunnerApplication>(*args) {
        setAdditionalProfiles("mcp")
    }
}
