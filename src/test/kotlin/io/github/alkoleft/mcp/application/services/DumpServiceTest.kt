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

package io.github.alkoleft.mcp.application.services

import io.github.alkoleft.mcp.application.actions.common.DumpAction
import io.github.alkoleft.mcp.application.actions.common.DumpMode
import io.github.alkoleft.mcp.application.actions.common.DumpResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class DumpServiceTest {
    private lateinit var dumpAction: DumpAction
    private lateinit var properties: ApplicationProperties
    private lateinit var sourceSetFactory: SourceSetFactory
    private lateinit var dumpService: DumpService
    private lateinit var mockSourceSet: SourceSet

    @BeforeEach
    fun setUp() {
        dumpAction = mockk()
        properties = mockk()
        sourceSetFactory = mockk()
        mockSourceSet = mockk()

        every { sourceSetFactory.createDesignerSourceSet() } returns mockSourceSet

        dumpService = DumpService(dumpAction, properties, sourceSetFactory)
    }

    @Test
    fun `should call run for FULL mode`() {
        // Arrange
        val expectedResult = DumpResult(
            message = "Выгрузка успешно",
            success = true,
            mode = DumpMode.FULL,
            duration = Duration.ZERO,
        )
        every {
            dumpAction.run(
                properties = properties,
                sourceSet = mockSourceSet,
                extension = null,
                allExtensions = false,
            )
        } returns expectedResult

        val request = DumpRequest(mode = DumpMode.FULL)

        // Act
        val result = dumpService.dump(request)

        // Assert
        assertTrue(result.success)
        assertEquals(DumpMode.FULL, result.mode)
        verify(exactly = 1) {
            dumpAction.run(
                properties = properties,
                sourceSet = mockSourceSet,
                extension = null,
                allExtensions = false,
            )
        }
    }

    @Test
    fun `should call runIncremental for INCREMENTAL mode`() {
        // Arrange
        val expectedResult = DumpResult(
            message = "Инкрементальная выгрузка успешно",
            success = true,
            mode = DumpMode.INCREMENTAL,
            duration = Duration.ZERO,
        )
        every {
            dumpAction.runIncremental(
                properties = properties,
                sourceSet = mockSourceSet,
                extension = null,
            )
        } returns expectedResult

        val request = DumpRequest(mode = DumpMode.INCREMENTAL)

        // Act
        val result = dumpService.dump(request)

        // Assert
        assertTrue(result.success)
        assertEquals(DumpMode.INCREMENTAL, result.mode)
        verify(exactly = 1) {
            dumpAction.runIncremental(
                properties = properties,
                sourceSet = mockSourceSet,
                extension = null,
            )
        }
    }

    @Test
    fun `should call runPartial for PARTIAL mode with objects`() {
        // Arrange
        val objects = listOf("Справочник.Номенклатура", "Документ.Заказ")
        val expectedResult = DumpResult(
            message = "Частичная выгрузка успешно",
            success = true,
            mode = DumpMode.PARTIAL,
            dumpedObjects = 2,
            duration = Duration.ZERO,
        )
        every {
            dumpAction.runPartial(
                properties = properties,
                sourceSet = mockSourceSet,
                objects = objects,
                extension = null,
            )
        } returns expectedResult

        val request = DumpRequest(mode = DumpMode.PARTIAL, objects = objects)

        // Act
        val result = dumpService.dump(request)

        // Assert
        assertTrue(result.success)
        assertEquals(DumpMode.PARTIAL, result.mode)
        assertEquals(2, result.dumpedObjects)
        verify(exactly = 1) {
            dumpAction.runPartial(
                properties = properties,
                sourceSet = mockSourceSet,
                objects = objects,
                extension = null,
            )
        }
    }

    @Test
    fun `should return error for PARTIAL mode without objects`() {
        // Arrange
        val request = DumpRequest(mode = DumpMode.PARTIAL, objects = emptyList())

        // Act
        val result = dumpService.dump(request)

        // Assert
        assertFalse(result.success)
        assertEquals(DumpMode.PARTIAL, result.mode)
        assertTrue(result.errors.any { it.contains("пуст") })
    }

    @Test
    fun `should pass extension parameter for extension dump`() {
        // Arrange
        val extensionName = "МоеРасширение"
        val expectedResult = DumpResult(
            message = "Выгрузка расширения успешно",
            success = true,
            mode = DumpMode.FULL,
            duration = Duration.ZERO,
        )
        every {
            dumpAction.run(
                properties = properties,
                sourceSet = mockSourceSet,
                extension = extensionName,
                allExtensions = false,
            )
        } returns expectedResult

        val request = DumpRequest(mode = DumpMode.FULL, extension = extensionName)

        // Act
        val result = dumpService.dump(request)

        // Assert
        assertTrue(result.success)
        verify(exactly = 1) {
            dumpAction.run(
                properties = properties,
                sourceSet = mockSourceSet,
                extension = extensionName,
                allExtensions = false,
            )
        }
    }

    @Test
    fun `should pass allExtensions flag`() {
        // Arrange
        val expectedResult = DumpResult(
            message = "Выгрузка всех расширений успешно",
            success = true,
            mode = DumpMode.FULL,
            duration = Duration.ZERO,
        )
        every {
            dumpAction.run(
                properties = properties,
                sourceSet = mockSourceSet,
                extension = null,
                allExtensions = true,
            )
        } returns expectedResult

        val request = DumpRequest(mode = DumpMode.FULL, allExtensions = true)

        // Act
        val result = dumpService.dump(request)

        // Assert
        assertTrue(result.success)
        verify(exactly = 1) {
            dumpAction.run(
                properties = properties,
                sourceSet = mockSourceSet,
                extension = null,
                allExtensions = true,
            )
        }
    }
}
