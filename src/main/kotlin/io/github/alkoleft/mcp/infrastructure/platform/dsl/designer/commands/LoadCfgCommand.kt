package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands

import java.nio.file.Path

/**
 * Команда LoadCfg с DSL функциональностью
 *
 * /LoadCfg <путь к файлу конфигурации> [/UpdateDBCfg]
 *
 * Загрузка конфигурации из файла .cf
 */
class LoadCfgCommand :
    ConfiguratorCommand(),
    UpdateDBCfgSupport {
    override val name: String = "LoadCfg"
    override val description: String = "Загрузка конфигурации из файла"

    // DSL параметры
    var configPath: Path? = null
    override var updateDBCfg: Boolean = false

    // DSL методы
    fun config(path: Path) {
        this.configPath = path
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Путь к файлу конфигурации (обязательный)
            configPath?.let { args.add(it.toString()) }

            // Параметр /UpdateDBCfg
            addUpdateDBCfgToArguments(args)

            return args
        }

    override val parameters: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()

            configPath?.let { params["configPath"] = it.toString() }

            // Параметр updateDBCfg
            addUpdateDBCfgToParameters(params)

            return params
        }
}
