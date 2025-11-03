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

/**
 * Элемент набора исходного кода
 */
data class SourceSetItem(
    val path: String,
    val name: String,
    val type: SourceSetType,
    val purpose: Set<SourceSetPurpose> = emptySet(),
) {
    init {
        validateSourceSetItem()
    }

    /**
     * Валидация элемента набора исходного кода
     */
    private fun validateSourceSetItem() {
        require(path.isNotBlank()) {
            "Путь элемента source set не может быть пустым"
        }
        require(name.isNotBlank()) {
            "Имя элемента source set не может быть пустым"
        }

        // Проверяем, что путь не содержит недопустимые символы
        require(!path.contains("..")) {
            "Путь source set не может содержать '..': $path"
        }

        // Проверяем, что путь не начинается с слеша (должен быть относительным)
        require(!path.startsWith("/")) {
            "Путь source set должен быть относительным: $path"
        }

        // Проверяем, что имя не содержит недопустимые символы
        require(!name.contains("/") && !name.contains("\\")) {
            "Имя source set не может содержать разделители пути: $name"
        }

        // Проверяем, что тип имеет допустимое значение
        require(type in SourceSetType.entries.toTypedArray()) {
            "Недопустимый тип source set: $type"
        }

        // Проверяем, что все назначения имеют допустимые значения
        purpose.forEach { purposeItem ->
            require(purposeItem in SourceSetPurpose.entries.toTypedArray()) {
                "Недопустимое назначение source set: $purposeItem"
            }
        }
    }
}
