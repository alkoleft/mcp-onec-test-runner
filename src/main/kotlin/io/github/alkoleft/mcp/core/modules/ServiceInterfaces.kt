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

package io.github.alkoleft.mcp.core.modules

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import java.nio.file.Path

/**
 * Build state management
 */
interface BuildStateManager {
    fun checkChanges(properties: ApplicationProperties): ChangesSet

    fun updateHashes(files: Map<Path, String>)
}

data class YaXUnitExecutionResult(
    val success: Boolean,
    val reportPath: Path?,
    val exitCode: Int,
    val standardOutput: String,
    val errorOutput: String,
    val duration: java.time.Duration,
)

enum class ReportFormat {
    JUNIT_XML,
}
