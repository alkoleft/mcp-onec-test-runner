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

package io.github.alkoleft.mcp.application.actions.launch

import io.github.alkoleft.mcp.application.actions.common.LaunchAction
import io.github.alkoleft.mcp.application.actions.common.LaunchRequest
import io.github.alkoleft.mcp.application.actions.common.LaunchResult
import io.github.alkoleft.mcp.application.core.UtilityType
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.alkoleft.mcp.infrastructure.utility.ifNoBlank
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

/**
 * Реализация LaunchAction для запуска приложений платформы 1С
 */
@Component
class UtilityLaunchAction(
    private val platformDsl: PlatformDsl,
    private val properties: ApplicationProperties,
) : LaunchAction {
    override fun run(request: LaunchRequest): LaunchResult {
        val utilityType =
            resolveUtilityType(request)
                ?: return LaunchResult(
                    success = false,
                    message = "Неизвестный тип приложения",
                    errors =
                        listOf(
                            "Неизвестный тип приложения: ${request.utilityType}. Доступные типы: ${
                                utilityTypeAliases.keys.joinToString(
                                    ", ",
                                )
                            }",
                        ),
                )

        logger.info { "Запуск приложения типа: $utilityType" }
        val launchResult =
            when (utilityType) {
                UtilityType.DESIGNER -> launchDesigner(request)
                UtilityType.THIN_CLIENT -> launchThinClient(request)
                UtilityType.THICK_CLIENT -> launchThickClient(request)
                else -> return LaunchResult(
                    success = false,
                    message = "Неподдерживаемый тип приложения",
                    errors = listOf("Неподдерживаемый тип приложения: $utilityType. Доступные типы: DESIGNER, THIN_CLIENT, THICK_CLIENT"),
                )
            }
        return LaunchResult(
            success = launchResult.success,
            message =
                if (launchResult.success) {
                    "Приложение успешно запущено, pid: ${launchResult.pid}"
                } else {
                    "Не удалось запустить приложение"
                },
            errors =
                if (launchResult.error != null) {
                    listOf(launchResult.error)
                } else {
                    emptyList()
                },
        )
    }

    private fun resolveUtilityType(request: LaunchRequest) = utilityTypeAliases[request.utilityType.lowercase()]

    private fun launchDesigner(request: LaunchRequest): ProcessResult {
        lateinit var result: ProcessResult

        platformDsl.designer {
            connect(properties.connection.connectionString)
            properties.connection.user?.ifNoBlank { user(it) }
            properties.connection.password?.ifNoBlank { password(it) }

            result = launch()
        }
        return result
    }

    private fun launchThickClient(request: LaunchRequest): ProcessResult {
        lateinit var result: ProcessResult

        platformDsl.enterprise(UtilityType.THICK_CLIENT) {
            connect(properties.connection.connectionString)
            properties.connection.user?.ifNoBlank { user(it) }
            properties.connection.password?.ifNoBlank { password(it) }

            result = launch()
        }
        return result
    }

    private fun launchThinClient(request: LaunchRequest): ProcessResult {
        lateinit var result: ProcessResult

        platformDsl.enterprise(UtilityType.THIN_CLIENT) {
            connect(properties.connection.connectionString)
            properties.connection.user?.ifNoBlank { user(it) }
            properties.connection.password?.ifNoBlank { password(it) }

            result = launch()
        }
        return result
    }
}

/**
 * Словарь псевдонимов для типов утилит
 * Исключены типы IBCMD и IBSRV
 */
private val utilityTypeAliases =
    mapOf(
        // DESIGNER
        "designer" to UtilityType.DESIGNER,
        "1cv8" to UtilityType.DESIGNER,
        "конфигуратор" to UtilityType.DESIGNER,
        // THIN_CLIENT
        "thin_client" to UtilityType.THIN_CLIENT,
        "1cv8c" to UtilityType.THIN_CLIENT,
        "тонкий клиент" to UtilityType.THIN_CLIENT,
        "тонкий" to UtilityType.THIN_CLIENT,
        // THICK_CLIENT
        "thick_client" to UtilityType.THICK_CLIENT,
        "толстый клиент" to UtilityType.THICK_CLIENT,
        "толстый" to UtilityType.THICK_CLIENT,
    )
