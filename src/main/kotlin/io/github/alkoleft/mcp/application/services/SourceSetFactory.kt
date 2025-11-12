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

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.ProjectFormat
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import org.springframework.stereotype.Service

/**
 * Фабрика для создания source set для различных форматов проектов
 *
 * Инкапсулирует логику создания source set для форматов EDT и DESIGNER,
 * обеспечивая правильное преобразование путей и структуры проектов.
 *
 * @param properties Свойства приложения с конфигурацией проекта
 */
@Service
class SourceSetFactory(
    private val properties: ApplicationProperties,
) {
    /**
     * Создает source set для формата EDT
     *
     * Если формат проекта EDT, создает source set на основе базового пути и исходных файлов.
     * Если формат проекта не EDT, возвращает пустой source set.
     *
     * @return Source set для EDT формата или пустой source set
     */
    fun createEdtSourceSet(): SourceSet =
        if (properties.format == ProjectFormat.EDT) {
            SourceSet(
                properties.basePath,
                properties.sourceSet,
            )
        } else {
            SourceSet.EMPTY
        }

    /**
     * Создает source set для формата DESIGNER
     *
     * Если формат проекта EDT, создает source set в рабочей директории с преобразованными путями
     * (путь к элементу заменяется на имя элемента). Если формат проекта DESIGNER,
     * создает source set на основе базового пути и исходных файлов без изменений.
     *
     * @return Source set для DESIGNER формата
     */
    fun createDesignerSourceSet(): SourceSet =
        if (properties.format == ProjectFormat.EDT) {
            SourceSet(
                properties.workPath,
                properties.sourceSet.map { it.copy(path = it.name) },
            )
        } else {
            SourceSet(
                properties.basePath,
                properties.sourceSet,
            )
        }
}
