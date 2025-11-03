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
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilities
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.InteractiveProcessExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Сервис для автозапуска 1C:EDT CLI при старте приложения
 *
 * Запускает 1C:EDT CLI и инициализирует интерактивную сессию.
 * Поддерживает single-flight запуск по запросу: один процесс и одна инициализация одновременно.
 */
@Service
@EnableAsync
class EdtCliStartService(
    private val properties: ApplicationProperties,
    private val utilityContext: PlatformUtilities,
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Управление жизненным циклом
    private val initMutex = Mutex()
    private var initJob: Deferred<InteractiveProcessExecutor?>? = null
    private var executorRef: InteractiveProcessExecutor? = null
    private var isAutoStartEnabled = false

    /**
     * Получает интерактивный исполнитель.
     * Если не запущен — запускает и ожидает инициализации.
     * Если идет инициализация — ожидает её завершения.
     */
    fun interactiveExecutor(): InteractiveProcessExecutor? =
        runBlocking {
            if (properties.format != ProjectFormat.EDT) {
                logger.warn { "EDT формат не выбран, запуск невозможен" }
                return@runBlocking null
            }
            val started = ensureStarted()
            started.await()
        }

    /**
     * Обработчик события готовности приложения: триггерит автозапуск без ожидания.
     */
    @EventListener(ApplicationReadyEvent::class)
    @Async
    fun onApplicationReady() {
        if (properties.format != ProjectFormat.EDT) {
            logger.info { "EDT формат не выбран, автозапуск пропущен" }
            return
        }
        if (!properties.tools.edtCli.autoStart) {
            logger.info { "Автозапуск 1C:EDT CLI отключен" }
            return
        }

        isAutoStartEnabled = true
        logger.info { "Запуск 1C:EDT CLI в фоновом режиме (автозапуск)" }
        coroutineScope.launch {
            try {
                ensureStarted().start()
            } catch (e: Exception) {
                logger.error(e) { "Ошибка при автозапуске 1C:EDT CLI" }
            }
        }
    }

    /**
     * Гарантирует, что процесс запущен или запускается. Возвращает общую задачу инициализации.
     */
    private suspend fun ensureStarted(): Deferred<InteractiveProcessExecutor?> =
        initMutex.withLock {
            // Если уже есть готовый исполнитель
            executorRef?.let { existing ->
                if (existing.available) return@withLock CompletableDeferred(existing)
            }

            // Если уже идет инициализация — возвращаем её задачу
            initJob?.let { existing ->
                if (existing.isActive) return@withLock existing
                if (existing.isCompleted) {
                    // Очистим, чтобы создать новую при следующем вызове
                    initJob = null
                }
            }

            // Создаем новую задачу инициализации (ленивую)
            val newJob =
                coroutineScope.async(start = CoroutineStart.LAZY) {
                    startAndInitialize()
                }
            initJob = newJob
            return@withLock newJob
        }

    /**
     * Реальный запуск процесса 1C:EDT CLI и инициализация интерактивного исполнителя.
     */
    private suspend fun startAndInitialize(): InteractiveProcessExecutor? =
        withContext(Dispatchers.IO) {
            try {
                logger.info { "Инициализация процесса 1C:EDT CLI" }

                // Проверяем наличие исполняемого файла EDT
                val executablePath = utilityContext.locateUtility(UtilityType.EDT_CLI).executablePath
                if (!executablePath.toFile().exists()) {
                    logger.error { "Исполняемый файл 1C:EDT CLI не найден: $executablePath" }
                    return@withContext null
                }

                // Создаем процесс 1C:EDT CLI
                val command =
                    buildList {
                        add(executablePath.toString())
                        val workDir = properties.tools.edtCli.workingDirectory
                        if (!workDir.isNullOrBlank()) {
                            add("-data")
                            add(workDir)
                        }
                    }
                val processBuilder = ProcessBuilder(command).redirectErrorStream(true)

                val process = processBuilder.start()
                logger.info {
                    "Процесс 1C:EDT CLI запущен с PID: ${process.pid()}. Команда запуска: ${
                        processBuilder.command().joinToString(" ")
                    }"
                }

                // Создаем интерактивный исполнитель
                val executor =
                    InteractiveProcessExecutor(
                        process,
                        InteractiveProcessExecutor.InteractiveProcessParams(
                            promptPattern = "1C:EDT>",
                            promptTimeoutMs = properties.tools.edtCli.startupTimeoutMs,
                            commandTimeoutMs = properties.tools.edtCli.commandTimeoutMs,
                        ),
                    )

                // Инициализируем процесс
                val initialized = executor.initialize()
                if (!initialized) {
                    logger.error { "Не удалось инициализировать 1C:EDT CLI процесс" }
                    process.destroyForcibly()
                    return@withContext null
                }

                // Фиксируем готовый исполнитель и запускаем мониторинг
                initMutex.withLock {
                    executorRef = executor
                    initJob = null
                }

                logger.info { "1C:EDT CLI процесс успешно запущен и инициализирован" }

                // Мониторинг — только если включен автозапуск (сохраняем прежнюю семантику)
                if (isAutoStartEnabled) {
                    coroutineScope.launch { launchProcessMonitor(process, executor) }
                }

                return@withContext executor
            } catch (e: Exception) {
                logger.error(e) { "Ошибка при запуске 1C:EDT CLI процесса" }
                // Снимем зависшую задачу
                initMutex.withLock { initJob = null }
                return@withContext null
            }
        }

    /**
     * Запускает мониторинг процесса 1C:EDT CLI в фоновом режиме
     */
    private suspend fun launchProcessMonitor(
        process: Process,
        interactiveExecutor: InteractiveProcessExecutor,
    ) = withContext(Dispatchers.IO) {
        try {
            while (process.isAlive && isAutoStartEnabled) {
                delay(5000)
                if (interactiveExecutor.isInitialized() && interactiveExecutor.isProcessRunning()) {
                    logger.trace { "1C:EDT CLI процесс работает нормально" }
                } else {
                    logger.warn { "1C:EDT CLI процесс не отвечает, перезапуск..." }
                    restartProcess()
                    break
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Ошибка в мониторинге 1C:EDT CLI процесса" }
        }
    }

    /**
     * Перезапускает процесс 1C:EDT CLI
     */
    private suspend fun restartProcess() {
        logger.info { "Перезапуск 1C:EDT CLI процесса" }
        initMutex.withLock {
            try {
                executorRef?.stopProcess()
            } catch (e: Exception) {
                logger.warn(e) { "Ошибка при остановке 1C:EDT CLI процесса" }
            } finally {
                executorRef = null
                initJob = null
            }
        }
        // Запускаем заново (без ожидания)
        ensureStarted().start()
    }

    /**
     * Принудительно перезапускает 1C:EDT CLI исполнитель и ожидает готовности
     */
    fun restartInteractiveExecutor(): InteractiveProcessExecutor? =
        runBlocking {
            logger.info { "Принудительный перезапуск 1C:EDT CLI исполнителя" }
            initMutex.withLock {
                try {
                    executorRef?.stopProcess()
                } catch (e: Exception) {
                    logger.warn(e) { "Ошибка при остановке текущего 1C:EDT CLI процесса" }
                } finally {
                    executorRef = null
                    initJob = null
                }
            }
            ensureStarted().await()
        }

    /**
     * Проверяет, запущен ли процесс 1C:EDT CLI
     */
    fun isProcessStarted(): Boolean = executorRef?.isProcessRunning() == true || (initJob?.isActive == true)

    /**
     * Проверяет, включен ли автозапуск
     */
    fun isAutoStartEnabled(): Boolean = isAutoStartEnabled

    /**
     * Проверяет состояние 1C:EDT CLI исполнителя
     */
    fun getExecutorStatus(): String =
        when {
            properties.format != ProjectFormat.EDT -> "EDT формат не выбран"
            executorRef?.available == true -> "Готов к работе"
            initJob?.isActive == true -> "Инициализация в процессе"
            executorRef == null -> "Не запущен"
            else -> "Неопределенное состояние"
        }
}
