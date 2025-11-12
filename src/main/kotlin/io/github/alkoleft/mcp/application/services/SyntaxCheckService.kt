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

package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.services.validation.Issue
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.infrastructure.designer.DesignerValidationLogParser
import io.github.alkoleft.mcp.infrastructure.edt.EdtValidationLogParser
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger { }

/**
 * Сервис для выполнения синтаксис-проверки исходников
 *
 * Предоставляет методы для выполнения синтаксис-проверки через различные инструменты:
 * - ЕДТ (validate)
 * - Конфигуратор (CheckConfig)
 * - Конфигуратор (CheckModules)
 */
data class DesignerConfigCheckRequest(
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
    val allExtensions: Boolean = false,
) {
    fun validate() {
        if (!extendedModulesCheck && (checkUseSynchronousCalls || checkUseModality)) {
            throw IllegalArgumentException("Параметры CheckUseSynchronousCalls и CheckUseModality требуют включения ExtendedModulesCheck")
        }
    }
}

data class DesignerModulesCheckRequest(
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
    val allExtensions: Boolean = false,
) {
    fun hasModes(): Boolean =
        thinClient ||
            webClient ||
            server ||
            externalConnection ||
            thickClientOrdinaryApplication ||
            mobileAppClient ||
            mobileAppServer ||
            mobileClient ||
            extendedModulesCheck
}

data class EdtCheckRequest(
    val projectName: String?,
)

@Service
class SyntaxCheckService(
    private val platformDsl: PlatformDsl,
    private val properties: ApplicationProperties,
    private val validationLogParser: DesignerValidationLogParser,
    private val edtValidationLogParser: EdtValidationLogParser,
) {
    /**
     * Выполняет синтаксис-проверку через Конфигуратор (CheckConfig)
     *
     * @return Результат проверки CheckConfig
     */
    fun checkDesigner(options: DesignerConfigCheckRequest = DesignerConfigCheckRequest()): SyntaxCheckResult {
        logger.info { "Выполнение синтаксис-проверки через Конфигуратор (CheckConfig)" }
        options.validate()
        val result =
            platformDsl.designer().checkConfig {
                if (options.configLogIntegrity) configLogIntegrity()
                if (options.incorrectReferences) incorrectReferences()
                if (options.thinClient) thinClient()
                if (options.webClient) webClient()
                if (options.mobileClient) mobileClient()
                if (options.server) server()
                if (options.externalConnection) externalConnection()
                if (options.externalConnectionServer) externalConnectionServer()
                if (options.mobileAppClient) mobileAppClient()
                if (options.mobileAppServer) mobileAppServer()
                if (options.thickClientManagedApplication) thickClientManagedApplication()
                if (options.thickClientServerManagedApplication) thickClientServerManagedApplication()
                if (options.thickClientOrdinaryApplication) thickClientOrdinaryApplication()
                if (options.thickClientServerOrdinaryApplication) thickClientServerOrdinaryApplication()
                if (options.mobileClientDigiSign) mobileClientDigiSign()
                if (options.distributiveModules) distributiveModules()
                if (options.unreferenceProcedures) unreferenceProcedures()
                if (options.handlersExistence) handlersExistence()
                if (options.emptyHandlers) emptyHandlers()
                if (options.extendedModulesCheck) extendedModulesCheck()
                if (options.checkUseSynchronousCalls) checkUseSynchronousCalls()
                if (options.checkUseModality) checkUseModality()
                if (options.unsupportedFunctional) unsupportedFunctional()
                options.extension?.let { extension(it) }
                if (options.allExtensions) allExtensions()
            }

        return processResultToSyntaxCheckResult(result)
    }

    /**
     * Выполняет синтаксис-проверку модулей через Конфигуратор (CheckModules)
     *
     * @return Результат проверки CheckModules
     */
    fun checkDesigner(options: DesignerModulesCheckRequest): SyntaxCheckResult {
        if (!options.hasModes()) {
            throw IllegalArgumentException("Не указан ни один режим проверки модулей Конфигуратора")
        }
        logger.info { "Выполнение синтаксис-проверки через Конфигуратор (CheckModules)" }
        val result =
            platformDsl.designer().checkModules {
                if (options.thinClient) thinClient()
                if (options.webClient) webClient()
                if (options.server) server()
                if (options.externalConnection) externalConnection()
                if (options.thickClientOrdinaryApplication) thickClientOrdinaryApplication()
                if (options.mobileAppClient) mobileAppClient()
                if (options.mobileAppServer) mobileAppServer()
                if (options.mobileClient) mobileClient()
                if (options.extendedModulesCheck) extendedModulesCheck()
                options.extension?.let { extension(it) }
                if (options.allExtensions) allExtensions()
            }
        return processResultToSyntaxCheckResult(result)
    }

    /**
     * Выполняет синтаксис-проверку через ЕДТ (validate)
     *
     * @return Результат проверки через ЕДТ
     * @throws IllegalStateException если не найдены проекты типа CONFIGURATION в sourceSet
     */
    fun checkEdt(request: EdtCheckRequest): SyntaxCheckResult {
        logger.info { "Выполнение синтаксис-проверки через ЕДТ (validate)" }

        val projectNames =
            if (!request.projectName.isNullOrBlank()) {
                listOf(request.projectName)
            } else {
                properties.sourceSet
                    .byPurpose(SourceSetPurpose.MAIN)
                    .map { it.name }
            }

        if (projectNames.isEmpty()) {
            throw IllegalStateException("Не найдены проекты для проверки в sourceSet")
        }

        val errors = mutableListOf<String?>()
        val outputs = mutableListOf<String?>()
        var success = true
        val dsl = platformDsl.edt()
        val results =
            projectNames
                .map {
                    val logPath = generateSyntaxCheckLogPath("edt")
                    logPath to
                        dsl.validate(
                            outputFile = logPath.toAbsolutePath().toString(),
                            projectNames = listOf(it),
                        )
                }.flatMap { (logPath, result) ->
                    success = success && result.success
                    errors.add(result.error)
                    outputs.add(result.output)
                    parseEdtAnalysis(logPath) ?: emptyList()
                }

        return SyntaxCheckResult(
            success = success,
            output = outputs.filterNotNull().joinToString("\n").ifBlank { null },
            error = errors.filterNotNull().joinToString("\n").ifBlank { null },
            0,
            issues = results,
        )
    }

    /**
     * Генерирует путь к файлу лога для синтаксис-проверки
     *
     * @param type Тип проверки (edt, designer_checkconfig, designer_checkmodules)
     * @return Путь к файлу лога
     */
    private fun generateSyntaxCheckLogPath(type: String): Path {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "syntax_check_${timestamp}_$type.log"
        val logsDir = Path.of("logs")
        if (!Files.exists(logsDir)) {
            Files.createDirectories(logsDir)
        }
        return logsDir.resolve(fileName)
    }

    /**
     * Преобразует ProcessResult в SyntaxCheckResult
     */
    private fun processResultToSyntaxCheckResult(result: ProcessResult): SyntaxCheckResult {
        val analysis = parseConfiguratorAnalysis(result.logFilePath)
        return SyntaxCheckResult(
            success = result.success,
            output = result.output,
            error = result.error,
            exitCode = result.exitCode,
            issues = analysis,
        )
    }

    private fun parseConfiguratorAnalysis(logFilePath: Path?): List<Issue>? {
        if (logFilePath == null) {
            return null
        }
        return try {
            validationLogParser.parse(logFilePath)
        } catch (error: Exception) {
            logger.warn(error) { "Не удалось разобрать лог проверки конфигуратора: $logFilePath" }
            null
        }
    }

    private fun parseEdtAnalysis(logFilePath: Path) =
        try {
            edtValidationLogParser.parse(logFilePath)
        } catch (error: Exception) {
            logger.warn(error) { "Не удалось разобрать лог проверки ЕДТ: $logFilePath" }
            null
        }
}

/**
 * Результат отдельной проверки синтаксиса
 *
 * Содержит детальную информацию о результате выполнения одной проверки,
 * включая сырой вывод команды, код возврата и путь к файлу логов.
 *
 * @param success Успешность проверки (true, если exitCode == 0)
 * @param output Сырой вывод команды из stdout
 * @param error Ошибки из stderr или error поля
 * @param exitCode Код возврата команды
 */
data class SyntaxCheckResult(
    val success: Boolean,
    val output: String?,
    val error: String?,
    val exitCode: Int,
    val issues: List<Issue>?,
)
