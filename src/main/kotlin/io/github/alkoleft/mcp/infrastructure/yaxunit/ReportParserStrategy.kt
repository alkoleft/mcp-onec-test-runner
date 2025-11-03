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

package io.github.alkoleft.mcp.infrastructure.yaxunit

import io.github.alkoleft.mcp.core.modules.GenericTestReport
import io.github.alkoleft.mcp.core.modules.ReportFormat
import java.io.InputStream

/**
 * Стратегия парсинга отчетов о тестировании
 * Поддерживает различные форматы отчетов через Strategy Pattern
 */
interface ReportParserStrategy {
    /**
     * Парсит отчет из входного потока
     */
    fun parse(input: InputStream): GenericTestReport

    /**
     * Проверяет, может ли стратегия обработать указанный формат
     */
    fun canHandle(format: ReportFormat): Boolean

    /**
     * Возвращает поддерживаемые форматы
     */
    fun getSupportedFormats(): Set<ReportFormat>

    /**
     * Определяет формат отчета из содержимого
     */
    fun detectFormat(input: InputStream): ReportFormat
}
