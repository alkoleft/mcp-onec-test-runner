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
 * Команда DumpExtensionToFiles с DSL функциональностью
 */
class DumpExtensionToFilesCommand : DesignerCommand() {
    override val name: String = "DumpExtensionToFiles"
    override val description: String = "Выгрузка расширения в файлы"

    // DSL параметры
    var targetPath: Path? = null
    var extension: String? = null
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

            // Параметр расширения (обязательный)
            extension?.let { ext ->
                args.add("-Extension")
                args.add(ext)
            }

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
