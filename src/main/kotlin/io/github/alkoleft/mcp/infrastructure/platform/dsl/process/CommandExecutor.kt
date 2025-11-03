package io.github.alkoleft.mcp.infrastructure.platform.dsl.process

import io.github.alkoleft.mcp.core.modules.ShellCommandResult

interface CommandExecutor {
    fun execute(commandArgs: List<String>): ShellCommandResult
}
