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

package io.github.alkoleft.mcp.application.actions.exceptions

/**
 * Структурированная иерархия ошибок с контекстом
 */
sealed class ActionError(
    message: String,
    cause: Throwable? = null,
    val context: Map<String, Any> = emptyMap(),
) : Exception(message, cause)

/**
 * Ошибка сборки проекта
 */
class BuildError(
    message: String,
    cause: Throwable? = null,
    context: Map<String, Any> = emptyMap(),
) : ActionError(message, cause, context)

/**
 * Ошибка конфигурации с контекстом
 */
class ConfigurationError(
    message: String,
    cause: Throwable? = null,
    context: Map<String, Any> = emptyMap(),
) : ActionError(message, cause, context)

/**
 * Ошибка валидации с контекстом
 */
class ValidationError(
    message: String,
    cause: Throwable? = null,
    context: Map<String, Any> = emptyMap(),
) : ActionError(message, cause, context)

/**
 * Ошибка анализа изменений с контекстом
 */
class AnalysisError(
    message: String,
    cause: Throwable? = null,
    context: Map<String, Any> = emptyMap(),
) : ActionError(message, cause, context)

/**
 * Ошибка выполнения тестов с контекстом
 */
class TestExecutionError(
    message: String,
    cause: Throwable? = null,
    context: Map<String, Any> = emptyMap(),
) : ActionError(message, cause, context)

// Обратная совместимость с существующими исключениями
sealed class ActionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

// Алиасы для обратной совместимости
typealias BuildException = BuildError
typealias AnalyzeException = AnalysisError
typealias TestExecuteException = TestExecutionError
