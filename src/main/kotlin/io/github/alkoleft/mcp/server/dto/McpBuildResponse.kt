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

import io.github.alkoleft.mcp.application.actions.common.ActionStepResult

/**
 * Результат сборки проекта в формате MCP
 *
 * Содержит информацию о результатах сборки проекта 1С:Предприятие, включая
 * статус, сообщение, время сборки и детали шагов выполнения (при ошибках).
 *
 * @param success Успешность сборки (true, если сборка завершилась без ошибок)
 * @param message Сообщение о результате сборки
 * @param buildTime Время выполнения сборки в миллисекундах
 * @param steps Список шагов выполнения сборки (заполняется только при ошибках для диагностики)
 */
data class McpBuildResponse(
    val success: Boolean,
    val message: String,
    val buildTime: Long? = null,
    val steps: List<ActionStepResult>? = null,
)
