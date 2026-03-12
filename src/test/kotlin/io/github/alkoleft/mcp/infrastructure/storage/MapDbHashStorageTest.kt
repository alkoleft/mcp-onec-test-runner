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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HashStorageMapDbModeTest {
    @Test
    fun `should disable memory mapping on Windows`() {
        assertFalse(shouldUseMemoryMapping(PlatformType.WINDOWS))
    }

    @Test
    fun `should keep memory mapping on non-Windows platforms`() {
        assertTrue(shouldUseMemoryMapping(PlatformType.LINUX))
        assertTrue(shouldUseMemoryMapping(PlatformType.MACOS))
    }
}
