package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import java.nio.file.Path

abstract class BasePlatformDsl<T : BasePlatformContext>(
    protected val context: T,
) {
    fun connect(connectionString: String) {
        context.connect(connectionString)
    }

    fun connectToServer(
        serverName: String,
        dbName: String,
    ) {
        context.connectToServer(serverName, dbName)
    }

    fun connectToFile(path: String) {
        context.connectToFile(path)
    }

    fun user(user: String) {
        context.user(user)
    }

    fun password(password: String) {
        context.password(password)
    }

    fun output(path: Path) {
        context.output(path)
    }

    fun language(code: String) {
        context.language(code)
    }

    fun localization(code: String) {
        context.localization(code)
    }

    fun disableStartupDialogs() {
        context.disableStartupDialogs()
    }

    fun disableStartupMessages() {
        context.disableStartupMessages()
    }

    fun noTruncate() {
        context.noTruncate()
    }
}
