package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands

/**
 * Команда CheckModules с DSL функциональностью
 */
class CheckModulesCommand : ConfiguratorCommand() {
    override val name: String = "CheckModules"
    override val description: String = "Проверка модулей конфигурации"

    // DSL параметры
    var extension: String? = null
    var allExtensions: Boolean = false
    var modules: String? = null
    var allModules: Boolean = false

    // DSL методы
    fun extension(name: String) {
        this.extension = name
    }

    fun allExtensions() {
        this.allExtensions = true
    }

    fun modules(modulesList: String) {
        this.modules = modulesList
    }

    fun allModules() {
        this.allModules = true
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Параметры расширения
            extension?.let { ext ->
                args.add("-Extension")
                args.add(ext)
            }
            if (allExtensions) args.add("-AllExtensions")

            // Параметры модулей
            modules?.let { mods ->
                args.add("-Modules")
                args.add(mods)
            }
            if (allModules) args.add("-AllModules")

            return args
        }

    override val parameters: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()

            extension?.let { params["extension"] = it }
            if (allExtensions) params["allExtensions"] = "true"
            modules?.let { params["modules"] = it }
            if (allModules) params["allModules"] = "true"

            return params
        }
}