package io.github.alkoleft.mcp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class McpYaxUnitRunnerApplication

fun main(args: Array<String>) {
    runApplication<McpYaxUnitRunnerApplication>(*args)
}
