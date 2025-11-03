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

import java.nio.file.Path
import kotlin.io.path.Path

class SourceSet(
    val basePath: Path = Path(""),
    items: Collection<SourceSetItem>? = emptyList(),
) : ArrayList<SourceSetItem>(items) {
    val configuration
        get() = find { it.type == SourceSetType.CONFIGURATION }
    val extensions
        get() = filter { it.type == SourceSetType.EXTENSION }

    fun subSourceSet(predicate: (SourceSetItem) -> Boolean) = SourceSet(basePath, filter(predicate))

    fun byName(name: String) = find { it.name == name } ?: throw ClassNotFoundException("Не обнаружен набор исходников с именем $name")

    fun pathByName(name: String): Path = basePath.resolve(byName(name).path)

    companion object {
        val EMPTY: SourceSet = SourceSet(Path(""))
    }
}
