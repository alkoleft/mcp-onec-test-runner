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

package io.github.alkoleft.mcp.infrastructure.platform.dsl

import io.github.alkoleft.mcp.application.actions.exceptions.BuildError
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.DesignerDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise.EnterpriseDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdDsl
import io.github.alkoleft.mcp.infrastructure.utility.ifNoBlank

private val FILE_PATH_PATTERN = "File\\s*=\\s*(['\"]?)([^'\";\\n]+)\\1\\s*".toRegex()

fun DesignerDsl.connect(properties: ApplicationProperties) {
    connect(properties.connection.connectionString)
    properties.connection.user?.ifNoBlank { user(it) }
    properties.connection.password?.ifNoBlank { password(it) }
}

fun EnterpriseDsl.connect(properties: ApplicationProperties) {
    connect(properties.connection.connectionString)
    properties.connection.user?.ifNoBlank { user(it) }
    properties.connection.password?.ifNoBlank { password(it) }
}

fun IbcmdDsl.connect(properties: ApplicationProperties) {
    dbPath = extractFilePath(properties.connection.connectionString)
        ?: throw BuildError("Не удалось определить путь к файлу из строки подключения")
    properties.connection.user?.ifNoBlank { user = it }
    properties.connection.password?.ifNoBlank { password = it }
}

private fun extractFilePath(connectionString: String) = FILE_PATH_PATTERN.find(connectionString)?.groupValues[2]
