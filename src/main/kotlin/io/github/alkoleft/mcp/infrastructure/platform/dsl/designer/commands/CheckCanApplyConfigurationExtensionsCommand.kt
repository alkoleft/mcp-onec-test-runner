package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands

/**
 * Команда CheckCanApplyConfigurationExtensions с DSL функциональностью
 */
class CheckCanApplyConfigurationExtensionsCommand : ConfiguratorCommand() {
    override val name: String = "CheckCanApplyConfigurationExtensions"
    override val description: String = "Проверка применимости расширений конфигурации"

    // DSL параметры
    var extension: String? = null
    var allZones: Boolean = false
    var zones: String? = null

    // DSL методы
    fun extension(name: String) {
        this.extension = name
    }

    fun allZones() {
        this.allZones = true
    }

    fun zones(zonesList: String) {
        this.zones = zonesList
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Параметры расширения
            extension?.let {
                args.add("-Extension")
                args.add(it)
            }
            if (allZones) args.add("-AllZones")
            zones?.let {
                args.add("-Z")
                args.add(it)
            }

            return args
        }

    override val parameters: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()

            extension?.let { params["extension"] = it }
            if (allZones) params["allZones"] = "true"
            zones?.let { params["zones"] = it }

            return params
        }
}
