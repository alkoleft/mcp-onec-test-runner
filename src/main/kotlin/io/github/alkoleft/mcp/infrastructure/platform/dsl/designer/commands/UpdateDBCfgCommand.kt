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

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.DynamicMode
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.SessionTerminateMode

/**
 * Команда UpdateDBCfg с DSL функциональностью
 *
 * /UpdateDBCfg [-BackgroundStart <режим>][-BackgroundCancel][-BackgroundFinish][-BackgroundResume]
 * [-BackgroundSuspend][-DynamicMode <режим>][-Visible][-WarningsAsErrors][-Server]
 * [-Extension <имя расширения>][-SessionTerminate <режим>]
 *
 * Обновление конфигурации в информационной базе.
 */
class UpdateDBCfgCommand : DesignerCommand() {
    override val name: String = "UpdateDBCfg"
    override val description: String = "Обновление конфигурации в информационной базе"

    // DSL параметры
    var backgroundStart: Boolean = false
    var backgroundCancel: Boolean = false
    var backgroundFinish: Boolean = false
    var backgroundResume: Boolean = false
    var backgroundSuspend: Boolean = false
    var dynamicMode: DynamicMode? = null
    var visible: Boolean = false
    var warningsAsErrors: Boolean = false
    var server: Boolean = false
    var extension: String? = null
    var sessionTerminate: SessionTerminateMode? = null

    // DSL методы
    fun backgroundStart(mode: DynamicMode = DynamicMode.PLUS) {
        this.backgroundStart = true
        this.dynamicMode = mode
    }

    fun backgroundCancel() {
        this.backgroundCancel = true
    }

    fun backgroundFinish(visible: Boolean = false) {
        this.backgroundFinish = true
        this.visible = visible
    }

    fun backgroundResume() {
        this.backgroundResume = true
    }

    fun backgroundSuspend() {
        this.backgroundSuspend = true
    }

    fun dynamicMode(mode: DynamicMode) {
        this.dynamicMode = mode
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

    fun extension(name: String) {
        this.extension = name
    }

    fun sessionTerminate(mode: SessionTerminateMode) {
        this.sessionTerminate = mode
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            // Параметры фонового обновления
            if (backgroundStart) {
                args.add("-BackgroundStart")
                dynamicMode?.let { args.add(it.value) }
            }
            if (backgroundCancel) args.add("-BackgroundCancel")
            if (backgroundFinish) {
                args.add("-BackgroundFinish")
                if (visible) args.add("-Visible")
            }
            if (backgroundResume) args.add("-BackgroundResume")
            if (backgroundSuspend) args.add("-BackgroundSuspend")

            // Параметры режима
            dynamicMode?.let { mode ->
                if (!backgroundStart) {
                    args.add("-DynamicMode")
                    args.add(mode.value)
                }
            }

            // Дополнительные параметры
            if (visible && !backgroundFinish) args.add("-Visible")
            if (warningsAsErrors) args.add("-WarningsAsErrors")
            if (server) args.add("-Server")

            // Параметры расширения
            extension?.let { ext ->
                args.add("-Extension")
                args.add(ext)
            }

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
