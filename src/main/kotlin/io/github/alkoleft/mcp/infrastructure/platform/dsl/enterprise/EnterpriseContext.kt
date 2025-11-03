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
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilities
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.V8Context
import java.nio.file.Path

/**
 * Контекст для работы с 1С:Предприятие
 */
class EnterpriseContext(
    platformContext: PlatformUtilities,
) : V8Context(platformContext) {
    var runArguments: String? = null

    /**
     * Строит аргументы для запуска 1С:Предприятие
     */
    override fun buildBaseArgs(): List<String> =
        buildCommonArgs(UtilityType.THIN_CLIENT, "ENTERPRISE")
            .also { args ->
                runArguments?.let {
                    args.add("/C")
                    args.add(it)
                }
            }

    fun buildBaseArgs(logPath: Path): List<String> = buildBaseArgs() + listOf("/Out", logPath.toString())
}
