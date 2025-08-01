package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands

fun Boolean.toYesNo() = if (this) "yes" else "no"