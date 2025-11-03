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

import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionRequest
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

/**
 * Конфигурация YaXUnit
 */
data class YaXUnitConfig(
    val filter: TestFilter?,
    val reportFormat: String,
    val reportPath: String,
    val closeAfterTests: Boolean,
    val showReport: Boolean,
    val logging: LoggingConfig,
)

/**
 * Конфигурация логирования
 */
data class LoggingConfig(
    val file: String? = null,
    val console: Boolean = false,
    val level: String = "info",
)

/**
 * Фильтр тестов
 */
data class TestFilter(
    val modules: List<String>? = null,
    val tests: List<String>? = null,
)

fun TestExecutionRequest.toConfig(): YaXUnitConfig =
    YaXUnitConfig(
        filter = filter(this),
        reportPath = reportPath(),
        logging =
            LoggingConfig(
                file = logPath(),
                console = false,
                level = "info",
            ),
        closeAfterTests = true,
        reportFormat = "jUnit",
        showReport = false,
    )

fun filter(request: TestExecutionRequest) =
    when (request) {
        is RunAllTestsRequest -> null
        is RunModuleTestsRequest -> TestFilter(modules = listOf(request.moduleName))
        is RunListTestsRequest -> TestFilter(tests = request.testNames)
    }

fun YaXUnitConfig.validate(): ValidationResult {
    logger.debug { "Проверка конфигурации YaXUnit" }

    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    // Проверяем обязательные поля
    if (reportFormat.isBlank()) {
        errors.add("Требуется указать формат отчета")
    }

    if (reportPath.isEmpty()) {
        errors.add("Путь к отчету не указан, будет использован путь по умолчанию")
    }

    // Проверяем формат отчета
    if (reportFormat != "jUnit" && reportFormat != "json" && reportFormat != "xml") {
        errors.add("Неподдерживаемый формат отчета: $reportFormat")
    }

    // Проверяем фильтры
    filter?.modules?.also {
        if (it.isEmpty()) {
            warnings.add("Указан пустой фильтр модулей")
        }
    }
    filter?.tests?.also {
        if (it.isEmpty()) {
            warnings.add("Указан пустой фильтр тестов")
        }
    }

    // Проверяем логирование
    if (logging.file == null && !logging.console) {
        warnings.add("Не указан вывод логирования")
    }

    val isValid = errors.isEmpty()

    logger.debug { "Проверка конфигурации завершена: isValid=$isValid, ошибок=${errors.size}, предупреждений=${warnings.size}" }

    return ValidationResult(
        isValid = isValid,
        errors = errors,
        warnings = warnings,
    )
}

/**
 * Результат валидации
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)
