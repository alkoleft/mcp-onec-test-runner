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

import io.github.alkoleft.mcp.application.services.validation.Issue

/**
 * Результат синтаксис-проверки в формате MCP
 *
 * Содержит информацию о результатах выполнения синтаксис-проверки через различные инструменты
 * (Конфигуратор и/или ЕДТ), включая детали каждой проверки и пути к файлам логов.
 *
 * @param success Общий успех (true, если все выполненные проверки успешны)
 * @param message Общее сообщение о результате
 * @param checkResult Результат проверки
 * @param errors Список ошибок выполнения
 */
data class McpSyntaxCheckResponse(
    val success: Boolean,
    val message: String,
    val checkResult: String? = null,
    val errors: List<String> = emptyList(),
    val issues: List<Issue>? = null,
    val duration: Long? = null,
)
