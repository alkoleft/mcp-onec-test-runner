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

package io.github.alkoleft.mcp.application.actions.common

import io.github.alkoleft.mcp.application.actions.ActionStepResult
import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult

abstract class ActionState {
    var success: Boolean = true
    val steps: MutableList<ActionStepResult> = mutableListOf()
}

fun ShellCommandResult.toActionStepResult(description: String) =
    ActionStepResult(
        description + if (success) ": успешно" else ": неудачно",
        success,
        fullError(),
        duration,
    )

fun ShellCommandResult.fullError() =
    when {
        this is ProcessResult && output.isNotEmpty() -> "$error\n$output"
        else -> error
    }
