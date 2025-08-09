package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.SessionTerminateMode
import java.nio.file.Path

/**
 * Команда ApplyCfg с DSL функциональностью
 */
class ApplyCfgCommand : ConfiguratorCommand() {
    override val name: String = "ApplyCfg"
    override val description: String = "Применение конфигурации"

    // DSL параметры
    var configPath: Path? = null
    var extension: String? = null
    var allExtensions: Boolean = false
    var force: Boolean = false
    var visible: Boolean = false
    var warningsAsErrors: Boolean = false
    var server: Boolean = false
    var sessionTerminate: SessionTerminateMode? = null

    // DSL методы
    fun config(path: Path) {
        this.configPath = path
    }

    fun extension(name: String) {
        this.extension = name
    }

    fun allExtensions() {
        this.allExtensions = true
    }

    fun force() {
        this.force = true
    }

    fun visible() {
        this.visible = true
    }

    fun warningsAsErrors() {
        this.warningsAsErrors = true
    }

    fun server() {
        this.server = true
    }

    fun sessionTerminate(mode: SessionTerminateMode) {
        this.sessionTerminate = mode
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Путь к конфигурации (обязательный)
            configPath?.let { args.add(it.toString()) }

            // Параметры расширений
            extension?.let { ext ->
                args.add("-Extension")
                args.add(ext)
            }
            if (allExtensions) args.add("-AllExtensions")

            // Дополнительные параметры
            if (force) args.add("-Force")
            if (visible) args.add("-Visible")
            if (warningsAsErrors) args.add("-WarningsAsErrors")
            if (server) args.add("-Server")

            // Параметр завершения сеансов
            sessionTerminate?.let { mode ->
                args.add("-SessionTerminate")
                args.add(mode.value)
            }

            return args
        }

    override val parameters: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()

            configPath?.let { params["configPath"] = it.toString() }
            extension?.let { params["extension"] = it }
            if (allExtensions) params["allExtensions"] = "true"
            if (force) params["force"] = "true"
            if (visible) params["visible"] = "true"
            if (warningsAsErrors) params["warningsAsErrors"] = "true"
            if (server) params["server"] = "true"
            sessionTerminate?.let { params["sessionTerminate"] = it.value }

            return params
        }
}
