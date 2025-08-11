package io.github.alkoleft.mcp.core.modules

import kotlin.time.Duration

interface ExecuteResult {
    val success: Boolean
    val duration: Duration
}

interface ShellCommandResult : ExecuteResult {
    val output: String
    val error: String?
    val exitCode: Int
}
