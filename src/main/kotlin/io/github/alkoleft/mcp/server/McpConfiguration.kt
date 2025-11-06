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

package io.github.alkoleft.mcp.server

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Конфигурация MCP сервера
 *
 * Настраивает интеграцию MCP сервера с Spring AI Framework. Регистрирует
 * [McpServer] как провайдер инструментов (tools) для MCP протокола.
 *
 * Основная функция:
 * - Создает [ToolCallbackProvider], который предоставляет доступ к инструментам
 *   MCP сервера через Spring AI Framework
 */
@Configuration
class McpConfiguration {
    /**
     * Создает провайдер инструментов MCP
     *
     * Регистрирует все методы [McpServer], помеченные аннотацией [org.springframework.ai.tool.annotation.Tool],
     * как доступные инструменты для MCP протокола. Это позволяет AI-ассистентам
     * вызывать методы сервера через протокол MCP.
     *
     * @param platformMcp Экземпляр MCP сервера, содержащий инструменты
     * @return Провайдер инструментов, который может быть использован Spring AI Framework
     */
    @Bean
    fun platformTools(platformMcp: McpServer): ToolCallbackProvider =
        MethodToolCallbackProvider
            .builder()
            .toolObjects(platformMcp)
            .build()
}
