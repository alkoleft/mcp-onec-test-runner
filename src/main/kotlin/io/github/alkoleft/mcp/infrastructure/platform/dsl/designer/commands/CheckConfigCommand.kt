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
 * Команда CheckConfig с DSL функциональностью
 */
class CheckConfigCommand : DesignerCommand() {
    override val name: String = "CheckConfig"
    override val description: String = "Централизованная проверка конфигурации"

    // DSL параметры
    var configLogIntegrity: Boolean = false
    var incorrectReferences: Boolean = false
    var thinClient: Boolean = false
    var webClient: Boolean = false
    var mobileClient: Boolean = false
    var server: Boolean = false
    var externalConnection: Boolean = false
    var externalConnectionServer: Boolean = false
    var mobileAppClient: Boolean = false
    var mobileAppServer: Boolean = false
    var thickClientManagedApplication: Boolean = false
    var thickClientServerManagedApplication: Boolean = false
    var thickClientOrdinaryApplication: Boolean = false
    var thickClientServerOrdinaryApplication: Boolean = false
    var mobileClientDigiSign: Boolean = false
    var distributiveModules: Boolean = false
    var unreferenceProcedures: Boolean = false
    var handlersExistence: Boolean = false
    var emptyHandlers: Boolean = false
    var extendedModulesCheck: Boolean = false
    var checkUseSynchronousCalls: Boolean = false
    var checkUseModality: Boolean = false
    var unsupportedFunctional: Boolean = false
    var extension: String? = null
    var allExtensions: Boolean = false

    // DSL методы
    fun configLogIntegrity() {
        this.configLogIntegrity = true
    }

    fun incorrectReferences() {
        this.incorrectReferences = true
    }

    fun thinClient() {
        this.thinClient = true
    }

    fun webClient() {
        this.webClient = true
    }

    fun mobileClient() {
        this.mobileClient = true
    }

    fun server() {
        this.server = true
    }

    fun externalConnection() {
        this.externalConnection = true
    }

    fun externalConnectionServer() {
        this.externalConnectionServer = true
    }

    fun mobileAppClient() {
        this.mobileAppClient = true
    }

    fun mobileAppServer() {
        this.mobileAppServer = true
    }

    fun thickClientManagedApplication() {
        this.thickClientManagedApplication = true
    }

    fun thickClientServerManagedApplication() {
        this.thickClientServerManagedApplication = true
    }

    fun thickClientOrdinaryApplication() {
        this.thickClientOrdinaryApplication = true
    }

    fun thickClientServerOrdinaryApplication() {
        this.thickClientServerOrdinaryApplication = true
    }

    fun mobileClientDigiSign() {
        this.mobileClientDigiSign = true
    }

    fun distributiveModules() {
        this.distributiveModules = true
    }

    fun unreferenceProcedures() {
        this.unreferenceProcedures = true
    }

    fun handlersExistence() {
        this.handlersExistence = true
    }

    fun emptyHandlers() {
        this.emptyHandlers = true
    }

    fun extendedModulesCheck() {
        this.extendedModulesCheck = true
    }

    fun checkUseSynchronousCalls() {
        this.checkUseSynchronousCalls = true
    }

    fun checkUseModality() {
        this.checkUseModality = true
    }

    fun unsupportedFunctional() {
        this.unsupportedFunctional = true
    }

    fun extension(name: String) {
        this.extension = name
    }

    fun allExtensions() {
        this.allExtensions = true
    }

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

            // Параметры расширения
            extension?.let { ext ->
                args.add("-Extension")
                args.add(ext)
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
