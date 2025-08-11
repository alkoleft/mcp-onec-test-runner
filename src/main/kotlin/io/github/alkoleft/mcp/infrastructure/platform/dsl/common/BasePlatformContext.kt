package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.utility.ifNoBlank
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Base context with common configuration for platform utilities (Enterprise/Designer)
 */
abstract class BasePlatformContext(
    protected val platformContext: PlatformUtilityContext,
) {
    protected var connectionString: String = ""
    protected var user: String? = null
    protected var password: String? = null
    protected var outputPath: Path? = null
    protected var language: String? = null
    protected var localization: String? = null
    protected var disableStartupDialogs: Boolean = false
    protected var disableStartupMessages: Boolean = false
    protected var noTruncate: Boolean = false

    fun connect(connectionString: String) {
        this.connectionString = "\"${connectionString.replace("\"", "\"\"")}\""
    }

    fun connectToServer(
        serverName: String,
        dbName: String,
    ) {
        this.connectionString = "Srvr=\"$serverName\";Ref=\"$dbName\";"
    }

    fun connectToFile(path: String) {
        this.connectionString = "File=\"$path\";"
    }

    fun user(user: String) {
        this.user = user
    }

    fun password(password: String) {
        this.password = password
    }

    fun output(path: Path) {
        this.outputPath = path
    }

    fun language(code: String) {
        this.language = code
    }

    fun localization(code: String) {
        this.localization = code
    }

    fun disableStartupDialogs() {
        this.disableStartupDialogs = true
    }

    fun disableStartupMessages() {
        this.disableStartupMessages = true
    }

    /**
     * Не очищает файл вывода при записи
     */
    fun noTruncate() {
        this.noTruncate = true
    }

    protected suspend fun buildCommonArgs(
        utilityType: UtilityType,
        mode: String,
    ): MutableList<String> {
        val args = mutableListOf<String>()
        val location = platformContext.locateUtility(utilityType)
        args.add(location.executablePath.toString())
        if (mode.isNotBlank()) {
            args.add(mode)
        }

        connectionString.ifNoBlank {
            args.add("/IBConnectionString")
            args.add(connectionString)
        }

        user?.ifNoBlank { args.add("/N\"$it\"") }
        password?.ifNoBlank { args.add("/P\"$it\"") }

        outputPath?.let {
            args.add("/Out$it")
            if (noTruncate) {
                args.add("-NoTruncate")
            }
        }
        language?.ifNoBlank { args.add("/L$it") }
        localization?.ifNoBlank { args.add("/VL$it") }

        if (disableStartupDialogs) args.add("/DisableStartupDialogs")
        if (disableStartupMessages) args.add("/DisableStartupMessages")

        return args
    }

    fun setResult(
        success: Boolean,
        output: String,
        error: String?,
        exitCode: Int,
        duration: Duration,
    ) {
        platformContext.setResult(success, output, error, exitCode, duration)
    }

    fun buildResult() = platformContext.buildResult()
}
