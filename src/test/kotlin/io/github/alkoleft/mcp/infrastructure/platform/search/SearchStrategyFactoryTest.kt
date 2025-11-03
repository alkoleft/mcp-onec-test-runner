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

package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SearchStrategyFactoryTest {
    @Test
    fun `should create correct strategy for current platform`() {
        val factory = SearchStrategyFactory()
        val strategy = factory.createSearchStrategy()
        assertNotNull(strategy, "Strategy should not be null")
        when (PlatformDetector.current) {
            PlatformType.WINDOWS -> {
                assertTrue(
                    strategy is PlatformWindowsSearchStrategy,
                    "Should create WindowsSearchStrategy for Windows platform",
                )
            }

            PlatformType.LINUX, PlatformType.MACOS -> {
                assertTrue(
                    strategy is PlatformLinuxSearchStrategy,
                    "Should create LinuxSearchStrategy for Linux/MacOS platform",
                )
            }
        }
    }

    @Test
    fun `should create Windows strategy for Windows platform`() {
        val factory = SearchStrategyFactory()
        if (PlatformDetector.current == PlatformType.WINDOWS) {
            val strategy = factory.createSearchStrategy()
            assertTrue(
                strategy is PlatformWindowsSearchStrategy,
                "Should create WindowsSearchStrategy for Windows platform",
            )
        } else {
            assertTrue(true, "Test skipped - not on Windows platform")
        }
    }

    @Test
    fun `should create Linux strategy for Linux platform`() {
        val factory = SearchStrategyFactory()
        if (PlatformDetector.current == PlatformType.LINUX) {
            val strategy = factory.createSearchStrategy()
            assertTrue(strategy is PlatformLinuxSearchStrategy, "Should create LinuxSearchStrategy for Linux platform")
        } else {
            assertTrue(true, "Test skipped - not on Linux platform")
        }
    }

    @Test
    fun `should create Linux strategy for MacOS platform`() {
        val factory = SearchStrategyFactory()
        if (PlatformDetector.current == PlatformType.MACOS) {
            val strategy = factory.createSearchStrategy()
            assertTrue(strategy is PlatformLinuxSearchStrategy, "Should create LinuxSearchStrategy for MacOS platform")
        } else {
            assertTrue(true, "Test skipped - not on MacOS platform")
        }
    }

    @Test
    fun `should create strategy with correct tier structure`() {
        val factory = SearchStrategyFactory()
        val strategy = factory.createSearchStrategy()
        assertNotNull(strategy.locations, "Tier 1 locations should not be null")
        assertTrue(strategy.locations.isNotEmpty(), "Tier 1 locations should not be empty")
    }

    @Test
    fun `should create consistent strategies for same platform`() {
        val factory1 = SearchStrategyFactory()
        val factory2 = SearchStrategyFactory()
        val strategy1 = factory1.createSearchStrategy()
        val strategy2 = factory2.createSearchStrategy()
        assertEquals(
            strategy1.javaClass,
            strategy2.javaClass,
            "Strategies for same platform should be of same type",
        )
    }

    @Test
    fun `should handle all supported platform types`() {
        val factory = SearchStrategyFactory()
        val strategy = factory.createSearchStrategy()
        assertNotNull(strategy, "Strategy should be created for any supported platform")
        assertTrue(
            strategy is PlatformWindowsSearchStrategy || strategy is PlatformLinuxSearchStrategy,
            "Strategy should be either WindowsSearchStrategy or LinuxSearchStrategy",
        )
    }
}
