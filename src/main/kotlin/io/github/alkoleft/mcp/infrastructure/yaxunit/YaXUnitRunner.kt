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

package io.github.alkoleft.mcp.infrastructure.yaxunit

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.YaXUnitExecutionResult
import io.github.alkoleft.mcp.core.modules.YaXUnitRunner
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.alkoleft.mcp.infrastructure.utility.ifNoBlank
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.Path

private val logger = KotlinLogging.logger { }

/**
 * Реализация YaXUnitRunner для запуска тестов через 1С:Предприятие
 * Интегрирован со стратегиями построения команд и обработки ошибок
 */
class YaXUnitRunner(
    private val platformDsl: PlatformDsl,
) : YaXUnitRunner {
    private val objectMapper = ObjectMapper()

    override fun executeTests(
        utilityLocation: UtilityLocation,
        request: TestExecutionRequest,
    ): YaXUnitExecutionResult {
        val startTime = Instant.now()
        logger.info { "Запуск выполнения тестов YaXUnit для ${request.javaClass.simpleName}" }

        // Создаем временную конфигурацию
        val (configPath, config) = createConfigFile(request)
        try {
            // Запускаем тесты через EnterpriseDsl
            logger.debug { "Выполнение тестов через EnterpriseDsl" }
            val result =
                executeTests(
                    request = request,
                    configPath = configPath,
                )

            logger.info { "Процесс завершен с кодом выхода: ${result.exitCode}" }

            val duration = Duration.between(startTime, Instant.now())
            logger.info { "Выполнение тестов завершено за ${duration.toSeconds()}с" }

            // Определяем путь к отчету
            val reportPath = Path(config.reportPath)
            if (Files.exists(reportPath)) {
                logger.info { "Отчет о тестах найден по пути: $reportPath" }
            } else {
                logger.warn { "Отчет о тестах не найден по ожидаемому пути" }
            }

            return YaXUnitExecutionResult(
                success = result.success,
                reportPath = reportPath,
                exitCode = result.exitCode,
                standardOutput = result.output,
                errorOutput = result.error ?: "",
                duration = duration,
            )
        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now())
            logger.error(e) { "Выполнение тестов YaXUnit завершилось с ошибкой после ${duration.toSeconds()}с" }
            return YaXUnitExecutionResult(
                success = false,
                reportPath = null,
                exitCode = -1,
                standardOutput = "",
                errorOutput = e.message ?: "Неизвестная ошибка",
                duration = duration,
            )
        }
    }

    private fun createConfigFile(request: TestExecutionRequest): Pair<Path, YaXUnitConfig> {
        val config = request.toConfig()
        config.validate().also {
            if (!it.isValid) {
                logger.warn { "Проверка конфигурации не пройдена: ${it.errors.joinToString(", ")}" }
            }
        }
        val configPath = configPath()
        objectMapper.writeValue(configPath.toFile(), config)

        return configPath to config
    }

    /**
     * Строит аргументы команды для запуска 1С:Предприятие
     */
    private fun executeTests(
        request: TestExecutionRequest,
        configPath: Path,
    ): ProcessResult =
        platformDsl
            .enterprise {
                connect(request.ibConnection)
                request.user?.ifNoBlank { user(it) }
                request.password?.ifNoBlank { password(it) }
                runArguments("RunUnitTests=${configPath.toAbsolutePath()}")
            }.run()
}
