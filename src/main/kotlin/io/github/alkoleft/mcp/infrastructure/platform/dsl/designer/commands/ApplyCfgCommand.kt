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

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.SessionTerminateMode
import java.nio.file.Path

/**
 * Команда ApplyCfg с DSL функциональностью
 */
class ApplyCfgCommand : DesignerCommand() {
    override val name: String = "ApplyCfg"
    override val description: String = "Применение конфигурации"

    // DSL параметры
    var configPath: Path? = null
    var extension: String? = null
    var allExtensions: Boolean = false
    var force: Boolean = false
    var visible: Boolean = false
    var warningsAsErrors: Boolean = false
    var server: Boolean = false
    var sessionTerminate: SessionTerminateMode? = null

    // DSL методы
    fun config(path: Path) {
        this.configPath = path
    }

    fun extension(name: String) {
        this.extension = name
    }

    fun allExtensions() {
        this.allExtensions = true
    }

    fun force() {
        this.force = true
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

    fun sessionTerminate(mode: SessionTerminateMode) {
        this.sessionTerminate = mode
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Путь к конфигурации (обязательный)
            configPath?.let { args.add(it.toString()) }

            // Параметры расширений
            extension?.let { ext ->
                args.add("-Extension")
                args.add(ext)
            }
            if (allExtensions) args.add("-AllExtensions")

            // Дополнительные параметры
            if (force) args.add("-Force")
            if (visible) args.add("-Visible")
            if (warningsAsErrors) args.add("-WarningsAsErrors")
            if (server) args.add("-Server")

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

            configPath?.let { params["configPath"] = it.toString() }
            extension?.let { params["extension"] = it }
            if (allExtensions) params["allExtensions"] = "true"
            if (force) params["force"] = "true"
            if (visible) params["visible"] = "true"
            if (warningsAsErrors) params["warningsAsErrors"] = "true"
            if (server) params["server"] = "true"
            sessionTerminate?.let { params["sessionTerminate"] = it.value }

            return params
        }
}
