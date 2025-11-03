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

import kotlin.math.max

/**
 * Version value type with universal parsing and lexicographic comparison.
 * Stores numeric version parts as a list and compares up to 4 segments.
 */
data class Version(
    val parts: List<Int>,
) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        val thisArr = toPaddedArray(parts)
        val otherArr = toPaddedArray(other.parts)
        for (i in 0 until 4) {
            val diff = thisArr[i] - otherArr[i]
            if (diff != 0) return if (diff < 0) -1 else 1
        }
        return 0
    }

    fun startsWith(mask: Version): Boolean {
        val minSize = minOf(parts.size, mask.parts.size)
        for (i in 0 until minSize) {
            if (parts[i] != mask.parts[i]) return false
        }
        return true
    }

    private fun toPaddedArray(src: List<Int>): IntArray {
        val size = src.size
        val padded = IntArray(4)
        val limit = minOf(4, size)
        var i = 0
        while (i < limit) {
            padded[i] = src[i]
            i++
        }
        while (i < 4) {
            padded[i] = 0
            i++
        }
        return padded
    }

    companion object {
        /**
         * Universal parser: extracts numeric tokens in order, ignoring non-numeric separators/suffixes.
         * Examples:
         * - "8.3.24.1234" -> [8,3,24,1234]
         * - "2025.1.0+656" -> [2025,1,0,656]
         * - "1c-edt-2025.1.0+656-x86_64" -> [1,2025,1,0,656,64] (callers should pre-extract the version substring)
         */
        fun parse(input: String): Version? {
            val matcher = Regex("\\d+").findAll(input)
            val numbers = matcher.mapNotNull { it.value.toIntOrNull() }.toList()
            if (numbers.isEmpty()) return null
            return Version(numbers.take(max(1, numbers.size)))
        }
    }
}
