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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.Command
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilities
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.V8Dsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import kotlin.time.Duration

/**
 * DSL для работы с 1С:Предприятие
 */
class EnterpriseDsl(
    utilityContext: PlatformUtilities,
    utilityType: UtilityType
) : V8Dsl<EnterpriseContext, Command>(EnterpriseContext(utilityContext, utilityType)) {
    fun runArguments(value: String) {
        context.runArguments = value
    }

    /**
     * Запускает 1С:Предприятие с указанными параметрами
     */
    fun run(): ProcessResult =
        try {
            val logPath = generateLogFilePath()
            val args = context.buildBaseArgs(logPath)

            ProcessExecutor().executeWithLogging(args, logPath)
        } catch (e: Exception) {
            ProcessResult(false, "", e.message ?: "Неизвестная ошибка", -1, Duration.ZERO)
        }

    fun launch(): ProcessResult = ProcessExecutor().launch(context.buildBaseArgs())
}
