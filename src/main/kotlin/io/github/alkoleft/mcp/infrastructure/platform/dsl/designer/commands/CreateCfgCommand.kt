package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands

import java.nio.file.Path

/**
 * Команда CreateCfg с DSL функциональностью
 */
class CreateCfgCommand : ConfiguratorCommand() {
    override val name: String = "CreateCfg"
    override val description: String = "Создание конфигурации"

    // DSL параметры
    var configPath: Path? = null
    var template: String? = null
    var force: Boolean = false
    var visible: Boolean = false

    // DSL методы
    fun config(path: Path) {
        this.configPath = path
    }

    fun template(name: String) {
        this.template = name
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

            // Параметр шаблона
            template?.let { tmpl ->
                args.add("-Template")
                args.add(tmpl)
            }

            // Дополнительные параметры
            if (force) args.add("-Force")
            if (visible) args.add("-Visible")

            return args
        }

    override val parameters: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()

            configPath?.let { params["configPath"] = it.toString() }
            template?.let { params["template"] = it }
            if (force) params["force"] = "true"
            if (visible) params["visible"] = "true"

            return params
        }
}