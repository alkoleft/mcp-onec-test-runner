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

package io.github.alkoleft.mcp.application.actions.common

import io.github.alkoleft.mcp.application.actions.change.ChangesSet
import io.github.alkoleft.mcp.application.actions.change.SourceSetChanges
import io.github.alkoleft.mcp.application.actions.test.yaxunit.GenericTestReport
import io.github.alkoleft.mcp.application.actions.test.yaxunit.TestExecutionRequest
import io.github.alkoleft.mcp.application.core.ShellCommandResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Интерфейс для сборки конфигурации и расширений
 *
 * Определяет контракт для выполнения сборки проекта 1С:Предприятие.
 * Реализации этого интерфейса могут использовать различные инструменты сборки
 * (например, конфигуратор или ibcmd).
 *
 * @param properties Свойства приложения, содержащие конфигурацию проекта
 * @param sourceSet Описание исходных файлов проекта (конфигурация и расширения)
 * @return Результат сборки с информацией об успешности, ошибках и времени выполнения
 */
interface BuildAction {
    /**
     * Выполняет сборку проекта
     *
     * @param properties Свойства приложения с конфигурацией
     * @param sourceSet Описание исходных файлов проекта
     * @return Результат сборки
     */
    fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult
}

/**
 * Результат сборки проекта
 *
 * Содержит информацию о результатах выполнения сборки проекта 1С:Предприятие,
 * включая статус, сообщения, ошибки, время выполнения и результаты для каждого
 * элемента source set.
 *
 * @param message Сообщение о результате сборки
 * @param success Успешность сборки (true, если сборка завершилась без ошибок)
 * @param errors Список ошибок, возникших во время сборки
 * @param duration Время выполнения сборки
 * @param steps Список шагов выполнения сборки для диагностики
 * @param sourceSet Результаты выполнения команд для каждого элемента source set (имя -> результат)
 */
data class BuildResult(
    override val message: String,
    override val success: Boolean,
    override val errors: List<String> = emptyList(),
    override val duration: Duration = Duration.ZERO,
    override val steps: List<ActionStepResult> = emptyList(),
    val sourceSet: Map<String, ShellCommandResult> = emptyMap(),
) : ActionResult

/**
 * Интерфейс для конвертации проектов между форматами
 *
 * Определяет контракт для конвертации проектов из одного формата в другой
 * (например, из формата EDT в формат DESIGNER).
 *
 * @param properties Свойства приложения
 * @param sourceSet Исходный source set для конвертации
 * @param destination Целевой source set для результата конвертации
 * @return Результат конвертации
 */
interface ConvertAction {
    /**
     * Выполняет конвертацию проекта
     *
     * @param properties Свойства приложения с конфигурацией
     * @param sourceSet Исходный source set
     * @param destination Целевой source set
     * @return Результат конвертации
     */
    fun run(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
        destination: SourceSet,
    ): ConvertResult
}

/**
 * Результат конвертации проекта
 *
 * Содержит информацию о результатах конвертации проекта из одного формата в другой.
 *
 * @param message Сообщение о результате конвертации
 * @param success Успешность конвертации
 * @param errors Список ошибок, возникших во время конвертации
 * @param duration Время выполнения конвертации
 * @param sourceSet Результаты выполнения команд для каждого элемента source set
 * @param steps Список шагов выполнения конвертации
 */
data class ConvertResult(
    override val message: String,
    override val success: Boolean,
    override val errors: List<String> = emptyList(),
    override val duration: Duration = Duration.ZERO,
    override val steps: List<ActionStepResult> = emptyList(),
    val sourceSet: Map<String, ShellCommandResult> = emptyMap(),
) : ActionResult

/**
 * Интерфейс для анализа изменений в проекте
 *
 * Определяет контракт для анализа изменений в файловой системе проекта
 * с целью определения необходимости пересборки.
 */
interface ChangeAnalysisAction {
    /**
     * Выполняет анализ изменений в проекте
     *
     * Анализирует файловую систему проекта и определяет, какие файлы были изменены,
     * добавлены или удалены с момента последней сборки.
     *
     * @return Результат анализа изменений
     */
    fun run(): ChangeAnalysisResult

    /**
     * Сохраняет состояние source set для инкрементальной сборки
     *
     * Сохраняет состояние изменений source set после успешной сборки для использования
     * в последующих анализах изменений.
     *
     * @param sourceSetChanges Изменения в source set
     * @param timeStamp Временная метка сборки
     * @param success Успешность сборки (true, если сборка прошла успешно)
     * @return true, если состояние успешно сохранено
     */
    fun saveSourceSetState(
        sourceSetChanges: SourceSetChanges,
        timeStamp: Long,
        success: Boolean,
    ): Boolean
}

/**
 * Интерфейс для запуска приложений 1С:Предприятие
 *
 * Определяет контракт для запуска различных приложений платформы 1С:Предприятие
 * (конфигуратор, тонкий клиент, толстый клиент).
 */
interface LaunchAction {
    /**
     * Запускает приложение указанного типа
     *
     * @param request Запрос на запуск приложения с указанием типа
     * @return Результат запуска приложения
     */
    fun run(request: LaunchRequest): LaunchResult
}

/**
 * Результат анализа изменений в проекте
 *
 * Содержит информацию об обнаруженных изменениях в файловой системе проекта.
 *
 * @param hasChanges Наличие изменений в проекте
 * @param changedFiles Множество путей к измененным файлам
 * @param changeTypes Типы изменений для каждого файла (путь -> (тип изменения, хеш))
 * @param sourceSetChanges Изменения, сгруппированные по source set (имя source set -> изменения)
 * @param steps Список шагов выполнения анализа
 * @param timestamp Временная метка выполнения анализа
 */
data class ChangeAnalysisResult(
    val hasChanges: Boolean,
    val changedFiles: Set<Path> = emptySet(),
    val changeTypes: ChangesSet = emptyMap(),
    val sourceSetChanges: Map<String, SourceSetChanges> = emptyMap(),
    val steps: List<ActionStepResult>,
    val timestamp: Long,
)

/**
 * Интерфейс для запуска тестов
 *
 * Определяет контракт для выполнения тестов YaXUnit в проекте 1С:Предприятие.
 */
interface RunTestAction {
    /**
     * Выполняет запуск тестов согласно запросу
     *
     * @param request Запрос на выполнение тестов (все тесты, тесты модуля и т.д.)
     * @return Результат выполнения тестов
     */
    fun run(request: TestExecutionRequest): RunTestResult
}

/**
 * Результат выполнения тестов
 *
 * Содержит полную информацию о результатах выполнения тестов YaXUnit, включая
 * отчет о тестировании, пути к логам и информацию об ошибках.
 *
 * @param success Успешность выполнения тестов
 * @param duration Время выполнения тестов
 * @param message Сообщение о результате выполнения
 * @param errors Список ошибок, возникших во время выполнения
 * @param steps Список шагов выполнения тестов
 * @param report Отчет о тестировании (может быть null, если отчет не удалось распарсить)
 * @param reportPath Путь к файлу отчета о тестировании
 * @param enterpriseLogPath Путь к логу 1С:Предприятие
 * @param logPath Путь к логу выполнения тестов YaXUnit
 */
data class RunTestResult(
    override val success: Boolean,
    override val duration: Duration,
    override val message: String,
    override val errors: List<String>,
    override val steps: List<ActionStepResult> = emptyList(),
    val report: GenericTestReport?,
    val reportPath: Path?,
    val enterpriseLogPath: String?,
    val logPath: String?,
) : ActionResult

/**
 * Базовый интерфейс для результатов выполнения действий
 *
 * Определяет общий контракт для всех результатов выполнения действий в системе.
 * Все конкретные результаты действий должны реализовывать этот интерфейс.
 */
interface ActionResult {
    /** Сообщение о результате выполнения */
    val message: String

    /** Успешность выполнения действия */
    val success: Boolean

    /** Список ошибок, возникших во время выполнения */
    val errors: List<String>

    /** Время выполнения действия */
    val duration: Duration

    /** Список шагов выполнения для диагностики */
    val steps: List<ActionStepResult>
}

/**
 * Результат выполнения отдельного шага действия
 *
 * Представляет результат выполнения одного шага в рамках действия
 * (например, загрузка конфигурации, выполнение команды и т.д.).
 *
 * @param message Сообщение о результате выполнения шага
 * @param success Успешность выполнения шага
 * @param error Текст ошибки (если шаг выполнен неуспешно)
 * @param duration Время выполнения шага
 */
data class ActionStepResult(
    val message: String,
    val success: Boolean,
    val error: String? = null,
    val duration: Duration,
)

/**
 * Запрос на запуск приложения 1С:Предприятие
 *
 * @param utilityType Псевдоним типа приложения для запуска (например, "designer", "1cv8c", "конфигуратор")
 */
data class LaunchRequest(
    val utilityType: String,
)

/**
 * Результат запуска приложения 1С:Предприятие
 *
 * Содержит информацию о результатах запуска приложения, включая идентификатор процесса.
 *
 * @param success Успешность запуска приложения
 * @param duration Время выполнения операции запуска
 * @param message Сообщение о результате запуска
 * @param errors Список ошибок, возникших при запуске
 * @param steps Список шагов выполнения запуска
 * @param processId Идентификатор запущенного процесса (PID), если запуск успешен
 */
data class LaunchResult(
    override val success: Boolean,
    override val duration: Duration = Duration.ZERO,
    override val message: String,
    override val errors: List<String>,
    override val steps: List<ActionStepResult> = emptyList(),
    val processId: Long? = null,
) : ActionResult
