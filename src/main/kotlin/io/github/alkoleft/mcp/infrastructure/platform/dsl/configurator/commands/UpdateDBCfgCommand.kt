package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands

import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.DynamicMode
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.SessionTerminateMode

/**
 * Команда UpdateDBCfg с DSL функциональностью
 *
 * /UpdateDBCfg [-BackgroundStart <режим>][-BackgroundCancel][-BackgroundFinish][-BackgroundResume]
 * [-BackgroundSuspend][-DynamicMode <режим>][-Visible][-WarningsAsErrors][-Server]
 * [-Extension <имя расширения>][-SessionTerminate <режим>]
 *
 * Обновление конфигурации в информационной базе.
 */
class UpdateDBCfgCommand : ConfiguratorCommand() {
    override val name: String = "UpdateDBCfg"
    override val description: String = "Обновление конфигурации в информационной базе"

    // DSL параметры
    var backgroundStart: Boolean = false
    var backgroundCancel: Boolean = false
    var backgroundFinish: Boolean = false
    var backgroundResume: Boolean = false
    var backgroundSuspend: Boolean = false
    var dynamicMode: DynamicMode? = null
    var visible: Boolean = false
    var warningsAsErrors: Boolean = false
    var server: Boolean = false
    var extension: String? = null
    var sessionTerminate: SessionTerminateMode? = null

    // DSL методы
    fun backgroundStart(mode: DynamicMode = DynamicMode.PLUS) {
        this.backgroundStart = true
        this.dynamicMode = mode
    }

    fun backgroundCancel() {
        this.backgroundCancel = true
    }

    fun backgroundFinish(visible: Boolean = false) {
        this.backgroundFinish = true
        this.visible = visible
    }

    fun backgroundResume() {
        this.backgroundResume = true
    }

    fun backgroundSuspend() {
        this.backgroundSuspend = true
    }

    fun dynamicMode(mode: DynamicMode) {
        this.dynamicMode = mode
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

    fun extension(name: String) {
        this.extension = name
    }

    fun sessionTerminate(mode: SessionTerminateMode) {
        this.sessionTerminate = mode
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Параметры фонового обновления
            if (backgroundStart) {
                args.add("-BackgroundStart")
                dynamicMode?.let { args.add(it.value) }
            }
            if (backgroundCancel) args.add("-BackgroundCancel")
            if (backgroundFinish) {
                args.add("-BackgroundFinish")
                if (visible) args.add("-Visible")
            }
            if (backgroundResume) args.add("-BackgroundResume")
            if (backgroundSuspend) args.add("-BackgroundSuspend")

            // Параметры режима
            dynamicMode?.let { mode ->
                if (!backgroundStart) {
                    args.add("-DynamicMode")
                    args.add(mode.value)
                }
            }

            // Дополнительные параметры
            if (visible && !backgroundFinish) args.add("-Visible")
            if (warningsAsErrors) args.add("-WarningsAsErrors")
            if (server) args.add("-Server")

            // Параметры расширения
            extension?.let { ext ->
                args.add("-Extension")
                args.add(ext)
            }

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

            if (backgroundStart) params["backgroundStart"] = "true"
            if (backgroundCancel) params["backgroundCancel"] = "true"
            if (backgroundFinish) params["backgroundFinish"] = "true"
            if (backgroundResume) params["backgroundResume"] = "true"
            if (backgroundSuspend) params["backgroundSuspend"] = "true"
            dynamicMode?.let { params["dynamicMode"] = it.value }
            if (visible) params["visible"] = "true"
            if (warningsAsErrors) params["warningsAsErrors"] = "true"
            if (server) params["server"] = "true"
            extension?.let { params["extension"] = it }
            sessionTerminate?.let { params["sessionTerminate"] = it.value }

            return params
        }
}