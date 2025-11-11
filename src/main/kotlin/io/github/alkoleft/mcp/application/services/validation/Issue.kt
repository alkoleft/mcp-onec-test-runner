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

package io.github.alkoleft.mcp.application.services.validation

sealed interface Issue {
    val path: String
    val message: String

    data class ModuleIssue(
        override val path: String,
        val extension: String?,
        val line: Int,
        val column: Int,
        override val message: String,
        val context: String?,
    ) : Issue

    data class ObjectIssue(
        override val path: String,
        val extension: String?,
        override val message: String,
    ) : Issue

    data class EdtValidationIssue(
        val level: EdtIssueLevel,
        val project: String?,
        val issueId: String?,
        override val path: String,
        val place: String?,
        override val message: String,
    ) : Issue
}

enum class EdtIssueLevel {
    TRIVIAL,
    MINOR,
    SIGNIFICANT,
    CRITICAL,
    BLOCKING,
    UNKNOWN,
    ;

    companion object {
        private val mapping: Map<String, EdtIssueLevel> =
            mapOf(
                "тривиальная" to TRIVIAL,
                "незначительная" to MINOR,
                "значительная" to SIGNIFICANT,
                "критическая" to CRITICAL,
                "блокирующая" to BLOCKING,
            )

        fun fromValue(value: String): EdtIssueLevel {
            val normalized: String = value.lowercase()
            return mapping[normalized] ?: UNKNOWN
        }
    }
}
