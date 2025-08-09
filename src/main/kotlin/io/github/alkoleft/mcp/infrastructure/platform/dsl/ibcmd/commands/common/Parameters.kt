package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common

interface Parameters {
    fun toArguments(): List<String>
}
