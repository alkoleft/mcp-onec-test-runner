package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands

import java.nio.file.Path

/**
 * Команда DeleteCfg с DSL функциональностью
 */
class DeleteCfgCommand : ConfiguratorCommand() {
    override val name: String = "DeleteCfg"
    override val description: String = "Удаление конфигурации"

    // DSL параметры
    var configPath: Path? = null
    var force: Boolean = false
    var visible: Boolean = false

    // DSL методы
    fun config(path: Path) {
        this.configPath = path
    }

    fun force() {
        this.force = true
    }

    fun visible() {
        this.visible = true
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Путь к конфигурации (обязательный)
            configPath?.let { args.add(it.toString()) }

            // Дополнительные параметры
            if (force) args.add("-Force")
            if (visible) args.add("-Visible")

            return args
        }

    override val parameters: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()

            configPath?.let { params["configPath"] = it.toString() }
            if (force) params["force"] = "true"
            if (visible) params["visible"] = "true"

            return params
        }
}
