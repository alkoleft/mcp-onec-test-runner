/*
 * This file is part of METR.
 *
 * Copyright (C) 2025 Aleksey Koryakin <alkoleft@gmail.com> and contributors.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * METR is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * METR is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with METR.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.LoadFormat
import java.nio.file.Path

/**
 * Команда LoadConfigFromFiles с DSL функциональностью
 *
 * /LoadConfigFromFiles <каталог загрузки> [-Extension <имя расширения>]
 * [-AllExtensions][-files "<файлы>"][-partial][-listFile <файл списка>][-format <режим>]
 * [-updateConfigDumpInfo][-NoCheck][-Archive <имя ZIP-архива>][/UpdateDBCfg]
 *
 * Загрузка конфигурации из файлов. Загрузка расширения в основную конфигурацию
 * (и наоборот) не поддерживается.
 */
class LoadConfigFromFilesCommand :
    DesignerCommand(),
    UpdateDBCfgSupport {
    override val name: String = "LoadConfigFromFiles"
    override val description: String = "Загрузка конфигурации из файлов"

    // DSL параметры
    var sourcePath: Path? = null
    var extension: String? = null
    var allExtensions: Boolean = false
    var files: String? = null
    var partial: Boolean = false
    var listFile: Path? = null
    var format: LoadFormat? = null
    var updateConfigDumpInfo: Boolean = false
    var noCheck: Boolean = false
    var archive: Path? = null
    override var updateDBCfg: Boolean = false

    // DSL методы
    fun fromPath(path: Path) {
        this.sourcePath = path
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

            // Параметр /UpdateDBCfg
            addUpdateDBCfgToArguments(args)

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

            // Параметр updateDBCfg
            addUpdateDBCfgToParameters(params)

            return params
        }
}
