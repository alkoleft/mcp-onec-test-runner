package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.ConfiguratorResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Path

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

        val results = mutableMapOf<String, ConfiguratorResult>()
        sourceSet.associateTo(results) { it.name to ConfiguratorResult.EMPTY }

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
                val result: ConfiguratorResult =
                    loadConfigFromFiles {
                        fromPath(properties.basePath.resolve(configuration.path))
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
            logger.info { "Загружаю ${extensions.size} расширений: ${extensions.joinToString(", ")}" }
            extensions.forEach {
                val result =
                    loadConfigFromFiles {
                        fromPath(properties.basePath.resolve(it.path))
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

    /**
     * Выполняет DSL для сборки только конфигурации
     */
    override suspend fun executeConfigurationBuildDsl(properties: ApplicationProperties): BuildResult {
        logger.info { "Формирую DSL для сборки конфигурации" }

        dsl.configurator {
            // Подключаемся к информационной базе
            connect(properties.connection.connectionString)

            // Отключаем диалоги и сообщения для автоматической работы
            disableStartupDialogs()
            disableStartupMessages()

            // Загружаем основную конфигурацию
            logger.info { "Загружаю основную конфигурацию" }
            val mainConfigPath = getMainConfiguration(properties.sourceSet)
            if (mainConfigPath != null) {
                loadConfigFromFiles {
                    fromPath(properties.basePath.resolve(mainConfigPath.path))
                    allExtensions()
                    updateConfigDumpInfo()
                }

                // Обновляем конфигурацию в базе данных
                logger.info { "Обновляю конфигурацию в базе данных" }
                updateDBCfg {
                    server()
                    warningsAsErrors()
                }

                // Проверяем конфигурацию
                logger.info { "Проверяю конфигурацию" }
                checkConfig {
                    configLogIntegrity()
                    incorrectReferences()
                    thinClient()
                    webClient()
                    server()
                    allExtensions()
                }

                logger.info { "Сборка конфигурации завершена успешно" }
            }
        }

        return BuildResult(
            success = false,
            errors = listOf("DSL сборки конфигурации не вернул результат"),
        )
    }

    /**
     * Выполняет DSL для сборки расширения
     */
    override suspend fun executeExtensionBuildDsl(
        extensionName: String,
        properties: ApplicationProperties,
    ): BuildResult {
        logger.info { "Формирую DSL для сборки расширения: $extensionName" }

        dsl.configurator {
            // Подключаемся к информационной базе
            connect(properties.connection.connectionString)

            // Отключаем диалоги и сообщения для автоматической работы
            disableStartupDialogs()
            disableStartupMessages()

            // Загружаем расширение
            logger.info { "Загружаю расширение: $extensionName" }
            val extensionPath = getExtensionPath(properties, extensionName)
            loadConfigFromFiles {
                fromPath(extensionPath)
                extension(extensionName)
                updateConfigDumpInfo()
            }

            // Проверяем возможность применения расширения
            logger.info { "Проверяю возможность применения расширения $extensionName" }
            checkCanApplyConfigurationExtensions {
                allZones()
            }

            // Обновляем конфигурацию в базе данных
            logger.info { "Обновляю конфигурацию с расширением $extensionName в базе данных" }
            updateDBCfg {
                server()
                warningsAsErrors()
            }

            logger.info { "Сборка расширения $extensionName завершена успешно" }
        }

        return BuildResult(
            success = false,
            errors = listOf("DSL сборки расширения не вернул результат"),
        )
    }

    /**
     * Получает путь к основной конфигурации
     */
    private fun getMainConfiguration(sourceSet: List<SourceSetItem>) =
        sourceSet
            .find { it.type == SourceSetType.CONFIGURATION }

    /**
     * Получает путь к расширению
     */
    private fun getExtensionPath(
        properties: ApplicationProperties,
        extensionName: String,
    ): Path {
        val extension =
            properties.sourceSet
                .find { it.type == SourceSetType.EXTENSION && it.path.contains(extensionName) }

        return extension?.let { properties.basePath.resolve(it.path) }
            ?: properties.basePath.resolve("Extensions").resolve("$extensionName.cf")
    }
}
