package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdPlan
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigApplyCommand
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.commands.config.ConfigImportCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID

private val logger = KotlinLogging.logger { }

/**
 * Реализация BuildAction для сборки через утилиту ibcmd.
 * Этот класс управляет выполнением команд ibcmd для импорта и применения конфигураций
 * в информационные базы 1С, поддерживая как основные конфигурации, так и расширения.
 */
class IbcmdBuildAction(
    dsl: PlatformDsl,
) : AbstractBuildAction(dsl) {
    /**
     * Извлекает путь к файлу из строки подключения, если это тип File=.
     * Поддерживает пути в одинарных/двойных кавычках или без кавычек.
     * Разрешает относительные пути на основе базового пути из свойств.
     *
     * @param connectionString Строка подключения для разбора.
     * @param properties Свойства приложения, содержащие базовый путь.
     * @return Разрешенный абсолютный путь к файлу или null, если не File= подключение.
     */
    private fun extractFilePath(
        connectionString: String,
        properties: ApplicationProperties,
    ): String? {
        if (!connectionString.startsWith("File=")) return null

        val pathMatch =
            Regex("""File=(?:'([^']+)'|"([^"]+)"|([^;]+));?""").find(connectionString)
                ?: return null

        val rawPath =
            when {
                !pathMatch.groupValues[1].isEmpty() -> pathMatch.groupValues[1] // Одинарные кавычки
                !pathMatch.groupValues[2].isEmpty() -> pathMatch.groupValues[2] // Двойные кавычки
                else -> pathMatch.groupValues[3] // Без кавычек
            }

        // Если относительный, разрешаем относительно basePath
        val basePath = properties.basePath.toString().removeSuffix("/")
        val resolvedPath =
            if (rawPath.startsWith('/') || rawPath.matches(Regex("""[A-Za-z]:[/\\].*"""))) {
                rawPath
            } else {
                "$basePath/$rawPath"
            }

        return resolvedPath
    }

    /**
     * Выполняет DSL сборки для заданного набора источников с использованием ibcmd.
     * Импортирует и применяет основную конфигурацию, если она присутствует, затем обрабатывает все расширения.
     * Собирает результаты и ошибки от каждой операции.
     *
     * @param properties Свойства приложения для подключения и путей.
     * @param sourceSet Набор источников, содержащий конфигурации и расширения для сборки.
     * @return BuildResult, указывающий на успех, результаты по элементам и ошибки.
     */
    override suspend fun executeBuildDsl(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult {
        logger.info { "Выполнение DSL сборки ibcmd" }

        val results = mutableMapOf<String, ShellCommandResult>()
        val errors = mutableListOf<String>()

        // Создание временного каталога данных для операций ibcmd
        val tempDataDir = System.getProperty("java.io.tmpdir") + "mcp-temp-" + UUID.randomUUID().toString().substring(0, 8)
        logger.debug { "Используется временный каталог данных: $tempDataDir" }

        // Загрузка основной конфигурации, если присутствует
        sourceSet.configuration?.also { configItem ->
            val path = sourceSet.basePath.resolve(configItem.path).toString()
            logger.info { "Импорт основной конфигурации из: $path" }

            val dbPathValue =
                extractFilePath(properties.connection.connectionString, properties)
                    ?: properties.connection.connectionString

            val ibUser =
                System.getenv("IBCMD_IB_USR") ?: properties.connection.user
                    ?: throw IllegalArgumentException("IBCMD_IB_USR или connection.user обязательны")
            val ibPassword =
                System.getenv("IBCMD_IB_PSW") ?: properties.connection.password
                    ?: throw IllegalArgumentException("IBCMD_IB_PSW или connection.password обязательны")

            // Построение плана DSL для импорта и применения основной конфигурации
            val plan =
                dsl.ibcmd {
                    data(tempDataDir)
                    dbPath(dbPathValue)
                    user(ibUser)
                    password(ibPassword)
                    config {
                        import(path)
                        apply {
                            force = true
                        }
                    }
                }

            val procResults = plan.execute()
            if (procResults.isNotEmpty()) {
                results[configItem.name] = procResults[0] // Предполагаем один результат для простоты, скорректировать при необходимости
                procResults.forEach { proc ->
                    if (!proc.success) {
                        errors.add(proc.error ?: "Неизвестная ошибка импорта/применения основной конфигурации")
                    }
                }
            }
        }

        // Загрузка расширений
        sourceSet.extensions.forEach { extItem ->
            val path = sourceSet.basePath.resolve(extItem.path).toString()
            logger.info { "Импорт расширения ${extItem.name} из: $path" }

            val dbPathValue =
                extractFilePath(properties.connection.connectionString, properties)
                    ?: properties.connection.connectionString

            val ibUser =
                System.getenv("IBCMD_IB_USR") ?: properties.connection.user
                    ?: throw IllegalArgumentException("IBCMD_IB_USR или connection.user обязательны")
            val ibPassword =
                System.getenv("IBCMD_IB_PSW") ?: properties.connection.password
                    ?: throw IllegalArgumentException("IBCMD_IB_PSW или connection.password обязательны")

            // Ручной план для импорта и применения расширения, чтобы обеспечить --extension в обеих командах
            val ibcmdContext = IbcmdContext(dsl.context)
            ibcmdContext.data(tempDataDir)
            ibcmdContext.dbPath(dbPathValue)
            ibcmdContext.user(ibUser)
            ibcmdContext.password(ibPassword)

            val importCmd =
                ConfigImportCommand(
                    path = path,
                    extension = extItem.name,
                )
            val applyCmd =
                ConfigApplyCommand(
                    extension = extItem.name,
                    force = true,
                )

            val manualPlan = IbcmdPlan(listOf(importCmd, applyCmd), ibcmdContext)

            val procResults = manualPlan.execute()
            if (procResults.isNotEmpty()) {
                results[extItem.name] = procResults[0] // Первый - импорт, но собираем все
                procResults.forEach { proc ->
                    if (!proc.success) {
                        errors.add(proc.error ?: "Неизвестная ошибка импорта/применения расширения ${extItem.name}")
                    }
                }
            }
        }

        val success = results.values.all { it.success }
        return BuildResult(
            success = success,
            sourceSet = results,
            errors = if (!success) errors else emptyList(),
        )
    }
}
