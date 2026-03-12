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

package io.github.alkoleft.mcp.infrastructure.storage

import io.github.alkoleft.mcp.application.core.PlatformType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HashStorageMapDbModeTest {
    @Test
    fun shouldDisableMemoryMappingOnWindows(): Unit {
        // Arrange
        val inputPlatform = PlatformType.WINDOWS
        val expectedResult = false

        // Act
        val actualResult = canUseMemoryMapping(inputPlatform)

        // Assert
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun shouldKeepMemoryMappingOnLinux(): Unit {
        // Arrange
        val inputPlatform = PlatformType.LINUX
        val expectedResult = true

        // Act
        val actualResult = canUseMemoryMapping(inputPlatform)

        // Assert
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun shouldKeepMemoryMappingOnMacOs(): Unit {
        // Arrange
        val inputPlatform = PlatformType.MACOS
        val expectedResult = true

        // Act
        val actualResult = canUseMemoryMapping(inputPlatform)

        // Assert
        assertEquals(expectedResult, actualResult)
    }
}
