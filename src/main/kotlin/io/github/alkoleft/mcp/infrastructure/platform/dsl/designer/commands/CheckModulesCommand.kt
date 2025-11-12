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

import io.github.alkoleft.mcp.infrastructure.utility.ifNoBlank

/**
 * Команда CheckModules с DSL функциональностью
 */
class CheckModulesCommand : DesignerCommand() {
    override val name: String = "CheckModules"
    override val description: String = "Проверка модулей конфигурации"

    // DSL параметры
    var thinClient: Boolean = false
    var webClient: Boolean = false
    var server: Boolean = false
    var externalConnection: Boolean = false
    var thickClientOrdinaryApplication: Boolean = false
    var mobileAppClient: Boolean = false
    var mobileAppServer: Boolean = false
    var mobileClient: Boolean = false
    var extendedModulesCheck: Boolean = false
    var extension: String? = null
    var allExtensions: Boolean = false

    // DSL методы
    fun thinClient() {
        thinClient = true
    }

    fun webClient() {
        webClient = true
    }

    fun server() {
        server = true
    }

    fun externalConnection() {
        externalConnection = true
    }

    fun thickClientOrdinaryApplication() {
        thickClientOrdinaryApplication = true
    }

    fun mobileAppClient() {
        mobileAppClient = true
    }

    fun mobileAppServer() {
        mobileAppServer = true
    }

    fun mobileClient() {
        mobileClient = true
    }

    fun extendedModulesCheck() {
        extendedModulesCheck = true
    }

    fun extension(name: String) {
        extension = name
    }

    fun allExtensions() {
        allExtensions = true
    }

    override val arguments: List<String>
        get() {
            val args = mutableListOf<String>()

            // Команда
            args.add("/$name")

            addModeArguments(args)
            requireModeSpecified()

            // Параметры расширения
            extension?.ifNoBlank { ext ->
                args.add("-Extension")
                args.add(ext)
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

    private fun addModeArguments(target: MutableList<String>) {
        if (thinClient) target.add("-ThinClient")
        if (webClient) target.add("-WebClient")
        if (server) target.add("-Server")
        if (externalConnection) target.add("-ExternalConnection")
        if (thickClientOrdinaryApplication) target.add("-ThickClientOrdinaryApplication")
        if (mobileAppClient) target.add("-MobileAppClient")
        if (mobileAppServer) target.add("-MobileAppServer")
        if (mobileClient) target.add("-MobileClient")
        if (extendedModulesCheck) target.add("-ExtendedModulesCheck")
    }

    private fun requireModeSpecified() {
        if (
            !thinClient &&
            !webClient &&
            !server &&
            !externalConnection &&
            !thickClientOrdinaryApplication &&
            !mobileAppClient &&
            !mobileAppServer &&
            !mobileClient &&
            !extendedModulesCheck
        ) {
            throw IllegalStateException("Для CheckModules должен быть указан хотя бы один режим проверки")
        }
    }
}
