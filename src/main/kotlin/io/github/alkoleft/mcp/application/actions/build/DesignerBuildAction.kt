package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

/**
 * Реализация BuildAction для сборки через конфигуратор 1С
 */
@Component
@ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "DESIGNER")
class DesignerBuildAction(
    dsl: PlatformDsl,
) : AbstractBuildAction(dsl) {
    /**
     * Выполняет единый DSL для полной сборки проекта
     */
    override suspend fun executeBuildDsl(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult {
        logger.debug { "Формирую единый DSL для сборки проекта" }

        val results = mutableMapOf<String, ShellCommandResult>()

        var buildResult: BuildResult? = null

        dsl.configurator {
            // Подключаемся к информационной базе
            connect(properties.connection.connectionString)

            // Отключаем диалоги и сообщения для автоматической работы
            disableStartupDialogs()
            disableStartupMessages()

            // Загружаем основную конфигурацию
            sourceSet.configuration?.also { configuration ->
                logger.info { "Загружаю основную конфигурацию" }
                val result: ShellCommandResult =
                    loadConfigFromFiles {
                        fromPath(sourceSet.basePath.resolve(configuration.path))
                        updateConfigDumpInfo()
                        updateDBCfg = true
                    }
                if (result.success) {
                    logger.info { "Конфигурация загружена успешно" }
                } else {
                    logger.info { "Не удалось загрузить конфигурацию" }
                }
                results[configuration.name] = result
            }

            // Загружаем расширения
            val extensions = sourceSet.extensions
            logger.info { "Загружаю ${extensions.size} расширений: ${extensions.joinToString(", ") { it.name }}" }
            extensions.forEach {
                val result =
                    loadConfigFromFiles {
                        fromPath(sourceSet.basePath.resolve(it.path))
                        extension(it.name)
                        updateConfigDumpInfo()
                        updateDBCfg = true
                    }
                results[it.name] = result
                if (result.success) {
                    logger.info { "Расширение ${it.name} загружено успешно" }
                } else {
                    logger.info { "Не удалось загрузить расширение ${it.name}" }
                }
            }

            // Обновляем конфигурацию в базе данных
            logger.info { "Обновляю конфигурацию в базе данных" }
            val updateResult = updateDBCfg { }

            if (!updateResult.success) {
                logger.error { "Ошибка обновления конфигурации: ${updateResult.error}" }
                buildResult =
                    BuildResult(
                        success = false,
                        sourceSet = results.toMap(),
                        errors = listOf("Ошибка обновления конфигурации: ${updateResult.error}"),
                    )
                return@configurator
            }

            logger.info { "Сборка завершена успешно" }
            buildResult =
                BuildResult(
                    success = true,
                    sourceSet = results.toMap(),
                )
        }

        // Если DSL не вернул результат, возвращаем ошибку
        return buildResult ?: BuildResult(
            success = false,
            errors = listOf("DSL сборки не вернул результат"),
            sourceSet = emptyMap(),
        )
    }
}
