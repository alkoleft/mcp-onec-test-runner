package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator

import java.nio.file.Path

/**
 * Базовая команда конфигуратора
 */
sealed class ConfiguratorCommand {
    abstract val name: String
    abstract val description: String
    abstract val arguments: List<String>
    abstract val parameters: Map<String, String>

    /**
     * Полное описание команды для отображения в плане
     */
    fun getFullDescription(): String {
        val params = if (parameters.isNotEmpty()) {
            " (${parameters.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
        } else ""
        return "$name$params - $description"
    }
}

/**
 * Команда LoadConfigFromFiles
 *
 * /LoadConfigFromFiles <каталог загрузки> [-Extension <имя расширения>]
 * [-AllExtensions][-files "<файлы>"][-partial][-listFile <файл списка>][-format <режим>]
 * [-updateConfigDumpInfo][-NoCheck][-Archive <имя ZIP-архива>]
 *
 * Загрузка конфигурации из файлов. Загрузка расширения в основную конфигурацию
 * (и наоборот) не поддерживается.
 */
data class LoadConfigFromFilesCommand(
    override val name: String = "LoadConfigFromFiles",
    override val description: String = "Загрузка конфигурации из файлов",
    var sourcePath: Path? = null,
    var extension: String? = null,
    var allExtensions: Boolean = false,
    var files: String? = null,
    var partial: Boolean = false,
    var listFile: Path? = null,
    var format: LoadFormat? = null,
    var updateConfigDumpInfo: Boolean = false,
    var noCheck: Boolean = false,
    var archive: Path? = null
) : ConfiguratorCommand() {

    /**
     * Создает команду LoadConfigFromFiles с настройкой через блок
     */
    companion object {
        fun create(block: LoadConfigFromFilesCommand.() -> Unit): LoadConfigFromFilesCommand {
            val command = LoadConfigFromFilesCommand()
            command.block()
            return command
        }
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Каталог загрузки (обязательный)
            sourcePath?.let { args.add(it.toString()) }

            // Параметры расширений
            extension?.let { ext ->
                args.add("-Extension")
                args.add(ext)
            }
            if (allExtensions) args.add("-AllExtensions")

            // Параметры файлов
            files?.let { fileList ->
                args.add("-files")
                args.add("\"$fileList\"")
            }
            if (partial) args.add("-partial")

            // Параметр списка файлов
            listFile?.let { file ->
                args.add("-listFile")
                args.add(file.toString())
            }

            // Параметр формата
            format?.let { fmt ->
                args.add("-format")
                args.add(fmt.value)
            }

            // Дополнительные параметры
            if (updateConfigDumpInfo) args.add("-updateConfigDumpInfo")
            if (noCheck) args.add("-NoCheck")
            archive?.let { arch ->
                args.add("-Archive")
                args.add(arch.toString())
            }

            return args
        }

    override val parameters: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()

            sourcePath?.let { params["sourcePath"] = it.toString() }
            extension?.let { params["extension"] = it }
            if (allExtensions) params["allExtensions"] = "true"
            files?.let { params["files"] = it }
            if (partial) params["partial"] = "true"
            listFile?.let { params["listFile"] = it.toString() }
            format?.let { params["format"] = it.value }
            if (updateConfigDumpInfo) params["updateConfigDumpInfo"] = "true"
            if (noCheck) params["noCheck"] = "true"
            archive?.let { params["archive"] = it.toString() }

            return params
        }
}

/**
 * Команда UpdateDBCfg
 */
data class UpdateDBCfgCommand(
    override val name: String = "UpdateDBCfg",
    override val description: String = "Обновление конфигурации в информационной базе",
    val backgroundStart: Boolean = false,
    val backgroundCancel: Boolean = false,
    val backgroundFinish: Boolean = false,
    val backgroundResume: Boolean = false,
    val backgroundSuspend: Boolean = false,
    val dynamicMode: DynamicMode? = null,
    val visible: Boolean = false,
    val warningsAsErrors: Boolean = false,
    val server: Boolean = false,
    val extension: String? = null,
    val sessionTerminate: SessionTerminateMode? = null
) : ConfiguratorCommand() {

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Параметры фонового обновления
            if (backgroundStart) {
                args.add("BackgroundStart")
                dynamicMode?.let { mode ->
                    args.add("-Dynamic${mode.value}")
                }
            }
            if (backgroundCancel) args.add("BackgroundCancel")
            if (backgroundFinish) {
                args.add("BackgroundFinish")
                if (visible) args.add("-Visible")
            }
            if (backgroundResume) args.add("BackgroundResume")
            if (backgroundSuspend) args.add("BackgroundSuspend")

            // Дополнительные параметры
            if (warningsAsErrors) args.add("-WarningsAsErrors")
            if (server) args.add("-Server")
            extension?.let { ext ->
                args.add("-Extension")
                args.add(ext)
            }
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

/**
 * Команда CheckCanApplyConfigurationExtensions
 */
data class CheckCanApplyConfigurationExtensionsCommand(
    override val name: String = "CheckCanApplyConfigurationExtensions",
    override val description: String = "Проверка применимости расширений конфигурации",
    val extension: String? = null,
    val allZones: Boolean = false,
    val zones: String? = null
) : ConfiguratorCommand() {

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

/**
 * Команда CheckConfig
 */
data class CheckConfigCommand(
    override val name: String = "CheckConfig",
    override val description: String = "Централизованная проверка конфигурации",
    val configLogIntegrity: Boolean = false,
    val incorrectReferences: Boolean = false,
    val thinClient: Boolean = false,
    val webClient: Boolean = false,
    val mobileClient: Boolean = false,
    val server: Boolean = false,
    val externalConnection: Boolean = false,
    val externalConnectionServer: Boolean = false,
    val mobileAppClient: Boolean = false,
    val mobileAppServer: Boolean = false,
    val thickClientManagedApplication: Boolean = false,
    val thickClientServerManagedApplication: Boolean = false,
    val thickClientOrdinaryApplication: Boolean = false,
    val thickClientServerOrdinaryApplication: Boolean = false,
    val mobileClientDigiSign: Boolean = false,
    val distributiveModules: Boolean = false,
    val unreferenceProcedures: Boolean = false,
    val handlersExistence: Boolean = false,
    val emptyHandlers: Boolean = false,
    val extendedModulesCheck: Boolean = false,
    val checkUseSynchronousCalls: Boolean = false,
    val checkUseModality: Boolean = false,
    val unsupportedFunctional: Boolean = false,
    val extension: String? = null,
    val allExtensions: Boolean = false
) : ConfiguratorCommand() {

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Параметры проверки
            if (configLogIntegrity) args.add("-ConfigLogIntegrity")
            if (incorrectReferences) args.add("-IncorrectReferences")
            if (thinClient) args.add("-ThinClient")
            if (webClient) args.add("-WebClient")
            if (mobileClient) args.add("-MobileClient")
            if (server) args.add("-Server")
            if (externalConnection) args.add("-ExternalConnection")
            if (externalConnectionServer) args.add("-ExternalConnectionServer")
            if (mobileAppClient) args.add("-MobileAppClient")
            if (mobileAppServer) args.add("-MobileAppServer")
            if (thickClientManagedApplication) args.add("-ThickClientManagedApplication")
            if (thickClientServerManagedApplication) args.add("-ThickClientServerManagedApplication")
            if (thickClientOrdinaryApplication) args.add("-ThickClientOrdinaryApplication")
            if (thickClientServerOrdinaryApplication) args.add("-ThickClientServerOrdinaryApplication")
            if (mobileClientDigiSign) args.add("-MobileClientDigiSign")
            if (distributiveModules) args.add("-DistributiveModules")
            if (unreferenceProcedures) args.add("-UnreferenceProcedures")
            if (handlersExistence) args.add("-HandlersExistence")
            if (emptyHandlers) args.add("-EmptyHandlers")
            if (extendedModulesCheck) args.add("-ExtendedModulesCheck")
            if (checkUseSynchronousCalls) args.add("-CheckUseSynchronousCalls")
            if (checkUseModality) args.add("-CheckUseModality")
            if (unsupportedFunctional) args.add("-UnsupportedFunctional")

            // Параметры расширений
            extension?.let {
                args.add("-Extension")
                args.add(it)
            }
            if (allExtensions) args.add("-AllExtensions")

            return args
        }

    override val parameters: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()

            if (configLogIntegrity) params["configLogIntegrity"] = "true"
            if (incorrectReferences) params["incorrectReferences"] = "true"
            if (thinClient) params["thinClient"] = "true"
            if (webClient) params["webClient"] = "true"
            if (mobileClient) params["mobileClient"] = "true"
            if (server) params["server"] = "true"
            if (externalConnection) params["externalConnection"] = "true"
            if (externalConnectionServer) params["externalConnectionServer"] = "true"
            if (mobileAppClient) params["mobileAppClient"] = "true"
            if (mobileAppServer) params["mobileAppServer"] = "true"
            if (thickClientManagedApplication) params["thickClientManagedApplication"] = "true"
            if (thickClientServerManagedApplication) params["thickClientServerManagedApplication"] = "true"
            if (thickClientOrdinaryApplication) params["thickClientOrdinaryApplication"] = "true"
            if (thickClientServerOrdinaryApplication) params["thickClientServerOrdinaryApplication"] = "true"
            if (mobileClientDigiSign) params["mobileClientDigiSign"] = "true"
            if (distributiveModules) params["distributiveModules"] = "true"
            if (unreferenceProcedures) params["unreferenceProcedures"] = "true"
            if (handlersExistence) params["handlersExistence"] = "true"
            if (emptyHandlers) params["emptyHandlers"] = "true"
            if (extendedModulesCheck) params["extendedModulesCheck"] = "true"
            if (checkUseSynchronousCalls) params["checkUseSynchronousCalls"] = "true"
            if (checkUseModality) params["checkUseModality"] = "true"
            if (unsupportedFunctional) params["unsupportedFunctional"] = "true"
            extension?.let { params["extension"] = it }
            if (allExtensions) params["allExtensions"] = "true"

            return params
        }
}

/**
 * Команда CheckModules
 */
data class CheckModulesCommand(
    override val name: String = "CheckModules",
    override val description: String = "Проверка модулей конфигурации",
    val thinClient: Boolean = false,
    val webClient: Boolean = false,
    val server: Boolean = false,
    val externalConnection: Boolean = false,
    val thickClientOrdinaryApplication: Boolean = false,
    val mobileAppClient: Boolean = false,
    val mobileAppServer: Boolean = false,
    val mobileClient: Boolean = false,
    val extendedModulesCheck: Boolean = false,
    val extension: String? = null,
    val allExtensions: Boolean = false
) : ConfiguratorCommand() {

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Параметры проверки
            if (thinClient) args.add("-ThinClient")
            if (webClient) args.add("-WebClient")
            if (server) args.add("-Server")
            if (externalConnection) args.add("-ExternalConnection")
            if (thickClientOrdinaryApplication) args.add("-ThickClientOrdinaryApplication")
            if (mobileAppClient) args.add("-MobileAppClient")
            if (mobileAppServer) args.add("-MobileAppServer")
            if (mobileClient) args.add("-MobileClient")
            if (extendedModulesCheck) args.add("-ExtendedModulesCheck")

            // Параметры расширений
            extension?.let {
                args.add("-Extension")
                args.add(it)
            }
            if (allExtensions) args.add("-AllExtensions")

            return args
        }

    override val parameters: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()

            if (thinClient) params["thinClient"] = "true"
            if (webClient) params["webClient"] = "true"
            if (server) params["server"] = "true"
            if (externalConnection) params["externalConnection"] = "true"
            if (thickClientOrdinaryApplication) params["thickClientOrdinaryApplication"] = "true"
            if (mobileAppClient) params["mobileAppClient"] = "true"
            if (mobileAppServer) params["mobileAppServer"] = "true"
            if (mobileClient) params["mobileClient"] = "true"
            if (extendedModulesCheck) params["extendedModulesCheck"] = "true"
            extension?.let { params["extension"] = it }
            if (allExtensions) params["allExtensions"] = "true"

            return params
        }
}

