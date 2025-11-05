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

package io.github.alkoleft.mcp.infrastructure.platform

import io.github.alkoleft.mcp.application.actions.test.yaxunit.TestExecutionError
import io.github.alkoleft.mcp.application.core.PlatformType
import io.github.alkoleft.mcp.application.core.UtilityLocation
import io.github.alkoleft.mcp.application.core.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrossPlatformUtilLocatorTest {
    @Test
    fun `should throw UtilNotFound when utility not found`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.DESIGNER, "8.3.24")
            }

        assertTrue(
            exception.utility.contains(UtilityType.DESIGNER.name),
            "Exception should contain correct utility type",
        )
    }

    @Test
    fun `should handle null version parameter`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.DESIGNER, null)
            }

        assertTrue(
            exception.utility.contains(UtilityType.DESIGNER.name),
            "Exception should contain correct utility type",
        )
    }

    @Test
    fun `should handle different utility types`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.IBCMD, "8.3.24")
            }

        assertTrue(exception.utility.contains(UtilityType.IBCMD.name), "Exception should contain correct utility type")
    }

    @Test
    fun `should validate utility location correctly`() {
        // Arrange
        val locator = UtilityLocator()
        val testLocation =
            UtilityLocation(
                executablePath = Paths.get("/non/existent/path"),
                version = "8.3.24",
                platformType = PlatformType.LINUX,
            )

        // Act
        val result =
            locator.validateUtility(testLocation)

        // Assert
        assertFalse(result, "Non-existent utility should fail validation")
    }

    @Test
    fun `should handle empty version string`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.DESIGNER, "")
            }

        assertTrue(
            exception.utility.contains(UtilityType.DESIGNER.name),
            "Exception should contain correct utility type",
        )
    }

    @Test
    fun `should handle invalid version format`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.DESIGNER, "invalid-version")
            }

        assertTrue(
            exception.utility.contains(UtilityType.DESIGNER.name),
            "Exception should contain correct utility type",
        )
    }

    @Test
    fun `should handle ENTERPRISE utility type`() {
        // Arrange
        val locator = UtilityLocator()

        // Act & Assert
        val exception =
            assertFailsWith<TestExecutionError.UtilNotFound> {
                locator.locateUtility(UtilityType.THIN_CLIENT, "8.3.24")
            }

        assertTrue(
            exception.utility.contains(UtilityType.THIN_CLIENT.name),
            "Exception should contain correct utility type",
        )
    }
}
