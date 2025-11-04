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

package io.github.alkoleft.mcp.application.actions.convert

import io.github.alkoleft.mcp.application.actions.ActionStepResult
import io.github.alkoleft.mcp.application.actions.ConvertAction
import io.github.alkoleft.mcp.application.actions.ConvertResult
import io.github.alkoleft.mcp.application.actions.common.toActionStepResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl

class EdtInteractiveConvertAction(
    private val dsl: PlatformDsl,
) : ConvertAction {
    override fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        destination: SourceSet,
    ): ConvertResult {
        val results = mutableMapOf<String, ShellCommandResult>()
        val steps = mutableListOf<ActionStepResult>()
        dsl.edt {
            sourceSet.forEach {
                val result =
                    export(
                        projectName = it.name,
                        configurationFiles = destination.pathByName(it.name),
                    )
                results[it.name] = result
                steps.add(result.toActionStepResult("Конвертация ${it.name}"))
                if (!result.success) {
                    return@edt
                }
            }
        }
        return ConvertResult(
            success = results.values.none { !it.success },
            sourceSet = results.toMap(),
            errors = steps.mapNotNull { it.error },
            steps = steps,
        )
    }
}
