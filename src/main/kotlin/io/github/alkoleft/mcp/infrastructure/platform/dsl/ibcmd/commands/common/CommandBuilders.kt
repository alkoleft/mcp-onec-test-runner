package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.common

import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdDsl

open class CommandBuilder(protected val dsl: IbcmdDsl) {
    protected val commands = mutableListOf<IbcmdCommand>()

    val result
        get() = commands.toList()

    protected fun <T : IbcmdCommand> configureAndExecute(
        command: T,
        configure: (T.() -> Unit)?,
    ) = dsl.configureAndExecute(command, configure)
}
