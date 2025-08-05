package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands

import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.LoadFormat
import java.nio.file.Path

/**
 * Команда DumpConfigToFiles с DSL функциональностью
 */
class DumpConfigToFilesCommand : ConfiguratorCommand() {
    override val name: String = "DumpConfigToFiles"
    override val description: String = "Выгрузка конфигурации в файлы"

    // DSL параметры
    var targetPath: Path? = null
    var extension: String? = null
    var allExtensions: Boolean = false
    var files: String? = null
    var partial: Boolean = false
    var listFile: Path? = null
    var format: LoadFormat? = null
    var updateConfigDumpInfo: Boolean = false
    var noCheck: Boolean = false
    var archive: Path? = null

    // DSL методы
    fun toPath(path: Path) {
        this.targetPath = path
    }

    fun extension(name: String) {
        this.extension = name
    }

    fun allExtensions() {
        this.allExtensions = true
    }

    fun files(fileList: String) {
        this.files = fileList
    }

    fun partial() {
        this.partial = true
    }

    fun listFile(file: Path) {
        this.listFile = file
    }

    fun format(format: LoadFormat) {
        this.format = format
    }

    fun updateConfigDumpInfo() {
        this.updateConfigDumpInfo = true
    }

    fun noCheck() {
        this.noCheck = true
    }

    fun archive(path: Path) {
        this.archive = path
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Каталог выгрузки (обязательный)
            targetPath?.let { args.add(it.toString()) }

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

            targetPath?.let { params["targetPath"] = it.toString() }
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