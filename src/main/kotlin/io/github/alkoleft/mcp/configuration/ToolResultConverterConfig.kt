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

package io.github.alkoleft.mcp.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.tool.execution.ToolCallResultConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.reflect.Type

/**
 * Конвертер результатов MCP tool, использующий настроенный ObjectMapper.
 *
 * В native image стандартный конвертер Spring AI не может сериализовать
 * Kotlin data-классы, т.к. его внутренний ObjectMapper не имеет KotlinModule.
 */
@Configuration
class ToolResultConverterConfig {

    @Bean
    fun toolCallResultConverter(objectMapper: ObjectMapper): ToolCallResultConverter {
        return ToolCallResultConverter { result, _ ->
            when {
                result == null -> "null"
                result is String -> result
                else -> objectMapper.writeValueAsString(result)
            }
        }
    }
}
