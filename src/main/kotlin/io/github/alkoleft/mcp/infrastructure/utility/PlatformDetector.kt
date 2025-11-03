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

package io.github.alkoleft.mcp.infrastructure.utility

import io.github.alkoleft.mcp.core.modules.PlatformType

/**
 * Platform detection utility for determining current operating system
 */
object PlatformDetector {
    val current: PlatformType by lazy {
        val osName = System.getProperty("os.name").lowercase()
        when {
            osName.contains("win") -> PlatformType.WINDOWS
            osName.contains("mac") -> PlatformType.MACOS
            else -> PlatformType.LINUX
        }
    }

    val isWindows: Boolean by lazy { current == PlatformType.WINDOWS }
    val isLinux: Boolean by lazy { current == PlatformType.LINUX }
    val isMacOS: Boolean by lazy { current == PlatformType.MACOS }
}
