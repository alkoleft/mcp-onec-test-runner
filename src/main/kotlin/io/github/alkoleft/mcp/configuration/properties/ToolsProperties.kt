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

package io.github.alkoleft.mcp.configuration.properties

/**
 * Настройки инструментов
 */
data class ToolsProperties(
    val builder: BuilderType = BuilderType.DESIGNER,
    val edtWorkSpace: String? = null,
    val edtCli: EdtCliProperties = EdtCliProperties(),
)

/**
 * Настройки EDT CLI
 */
data class EdtCliProperties(
    val autoStart: Boolean = false,
    val version: String = "latest",
    val interactiveMode: Boolean = true,
    val workingDirectory: String? = null,
    val startupTimeoutMs: Long = 30000, // 30 секунд по умолчанию
    val commandTimeoutMs: Long = 300000, // 5 минут по умолчанию
    val readyCheckTimeoutMs: Long = 5000, // 5 секунд для проверки готовности
)
