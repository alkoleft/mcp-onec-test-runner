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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

class ApplicationPropertiesValidationTest {
    @Test
    fun `should validate correct configuration`() {
        val tempDir = Files.createTempDirectory("test")
        val configPath = tempDir.resolve("config")
        Files.createDirectory(configPath)

        val properties =
            ApplicationProperties(
                basePath = tempDir,
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "config",
                                    name = "Configuration",
                                    type = SourceSetType.CONFIGURATION,
                                ),
                            ),
                    ),
                connection =
                    ConnectionProperties(
                        connectionString = "File=test.db;",
                    ),
                platformVersion = "8.3.20.1234",
                tools = ToolsProperties(BuilderType.DESIGNER),
            )

        // Если валидация прошла успешно, тест не должен выбросить исключение
        assertEquals(tempDir, properties.basePath)
        assertEquals("8.3.20.1234", properties.platformVersion)
    }

    @Test
    fun `should throw exception for non-existent base path`() {
        val nonExistentPath = Path.of("/non/existent/path")

        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = nonExistentPath,
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "config",
                                    name = "Configuration",
                                    type = SourceSetType.CONFIGURATION,
                                ),
                            ),
                    ),
                connection = ConnectionProperties(connectionString = "File=test.db;"),
            )
        }
    }

    @Test
    fun `should throw exception for empty source set`() {
        val tempDir = Files.createTempDirectory("test")

        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = tempDir,
                sourceSet = SourceSet(),
                connection = ConnectionProperties(connectionString = "File=test.db;"),
            )
        }
    }

    @Test
    fun `should throw exception for missing configuration source set`() {
        val tempDir = Files.createTempDirectory("test")

        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = tempDir,
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "extensions",
                                    name = "Extensions",
                                    type = SourceSetType.EXTENSION,
                                ),
                            ),
                    ),
                connection = ConnectionProperties(connectionString = "File=test.db;"),
            )
        }
    }

    @Test
    fun `should throw exception for empty connection string`() {
        val tempDir = Files.createTempDirectory("test")
        val configPath = tempDir.resolve("config")
        Files.createDirectory(configPath)

        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = tempDir,
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "config",
                                    name = "Configuration",
                                    type = SourceSetType.CONFIGURATION,
                                ),
                            ),
                    ),
                connection = ConnectionProperties(connectionString = ""),
            )
        }
    }

    @Test
    fun `should throw exception for invalid platform version`() {
        val tempDir = Files.createTempDirectory("test")
        val configPath = tempDir.resolve("config")
        Files.createDirectory(configPath)

        assertThrows<IllegalArgumentException> {
            ApplicationProperties(
                basePath = tempDir,
                sourceSet =
                    SourceSet(
                        items =
                            listOf(
                                SourceSetItem(
                                    path = "config",
                                    name = "Configuration",
                                    type = SourceSetType.CONFIGURATION,
                                ),
                            ),
                    ),
                connection = ConnectionProperties(connectionString = "File=test.db;"),
                platformVersion = "invalid-version",
            )
        }
    }

    @Test
    fun `should validate source set item with invalid path`() {
        assertThrows<IllegalArgumentException> {
            SourceSetItem(
                path = "",
                name = "Test",
                type = SourceSetType.CONFIGURATION,
            )
        }
    }

    @Test
    fun `should validate source set item with invalid name`() {
        assertThrows<IllegalArgumentException> {
            SourceSetItem(
                path = "config",
                name = "",
                type = SourceSetType.CONFIGURATION,
            )
        }
    }
}
