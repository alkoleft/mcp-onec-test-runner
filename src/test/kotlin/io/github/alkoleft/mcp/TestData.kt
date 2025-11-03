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

package io.github.alkoleft.mcp

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ConnectionProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.configuration.properties.ToolsProperties
import kotlin.io.path.Path

const val SOURCE_PATH = "/home/akoryakin@dellin.local/Загрузки/sources"
const val IB_PATH = "/home/common/develop/file-data-base/YAxUnit"
const val VERSION = "8.3.22.1709"

/**
 * Создает тестовые свойства приложения для тестирования
 */
fun testApplicationProperties(): ApplicationProperties =
    ApplicationProperties(
        basePath = Path(SOURCE_PATH),
        sourceSet =
            SourceSet(
                items =
                    listOf(
                        SourceSetItem(
                            path = "configuration",
                            name = "configuration",
                            type = SourceSetType.CONFIGURATION,
                            purpose = setOf(SourceSetPurpose.MAIN),
                        ),
                        SourceSetItem(
                            path = "yaxunit",
                            name = "yaxunit",
                            type = SourceSetType.EXTENSION,
                            purpose = setOf(SourceSetPurpose.YAXUNIT),
                        ),
                        SourceSetItem(
                            path = "tests",
                            name = "tests",
                            type = SourceSetType.EXTENSION,
                            purpose = setOf(SourceSetPurpose.TESTS),
                        ),
                    ),
            ),
        connection =
            ConnectionProperties(
                connectionString = "File=\"$IB_PATH\";",
            ),
        platformVersion = VERSION,
        tools = ToolsProperties(),
    )
