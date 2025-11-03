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

/**
 * Команда CheckModules с DSL функциональностью
 */
class CheckModulesCommand : DesignerCommand() {
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
