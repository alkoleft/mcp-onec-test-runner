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
 * Команда CheckCanApplyConfigurationExtensions с DSL функциональностью
 */
class CheckCanApplyConfigurationExtensionsCommand : DesignerCommand() {
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
