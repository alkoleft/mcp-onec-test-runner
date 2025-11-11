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

package io.github.alkoleft.mcp.server.dto

import io.github.alkoleft.mcp.application.actions.common.RunTestResult
import io.github.alkoleft.mcp.application.services.SyntaxCheckResult
import kotlin.time.Duration

/**
 * Преобразует результат выполнения тестов в формат MCP-ответа
 *
 * Расширяет функциональность [RunTestResult], преобразуя внутренний формат результата
 * в формат, понятный MCP-клиентам. Извлекает статистику тестов, детали выполнения
 * и информацию об ошибках из отчета.
 *
 * @receiver Результат выполнения тестов, полученный от LauncherService
 * @return MCP-ответ с результатами выполнения тестов
 */
fun RunTestResult.toResponse() =
    McpTestResponse(
        success = success,
        message = message,
        logFile = logPath,
        enterpriseLogPath = enterpriseLogPath,
        totalTests = report?.summary?.totalTests,
        passedTests = report?.summary?.passed,
        failedTests = report?.summary?.failed,
        executionTime = duration.inWholeMilliseconds,
        testDetail = report?.testSuites,
        steps = if (success) null else steps,
        errors = errors,
    )

fun SyntaxCheckResult.toResponse(
    checkName: String,
    duration: Duration,
) = if (issues.isNullOrEmpty()) {
    McpSyntaxCheckResponse(
        success = success,
        message = if (success) "Проверка $checkName выполнена успешно" else "Проверка $checkName завершилась с ошибками",
        checkResult = if (!success) output else null,
        errors = if (success) emptyList() else listOf(error ?: "Неизвестная ошибка"),
        issues = issues,
        duration = duration.inWholeMilliseconds,
    )
} else {
    McpSyntaxCheckResponse(
        success = success,
        message = "Проверка $checkName завершилась с ошибками",
        issues = issues,
        duration = duration.inWholeMilliseconds,
    )
}
