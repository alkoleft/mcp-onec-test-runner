package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildAction
import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.application.actions.exceptions.BuildException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSetItem
import io.github.alkoleft.mcp.configuration.properties.SourceSetPurpose
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.ConfiguratorResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Реализация BuildAction для сборки через конфигуратор 1С
 */
class DesignerBuildAction(
    private val dsl: PlatformUtilityDsl
) : BuildAction {

    override suspend fun build(projectProperties: ApplicationProperties): BuildResult {
        val startTime = Instant.now()
        logger.info { "Начинаю полную сборку проекта: ${projectProperties.basePath}" }

        try {
            // Формируем единый DSL для сборки
            val buildResult = executeBuildDsl(projectProperties)

            val duration = Duration.between(startTime, Instant.now())
            logger.info { "Сборка завершена. Успех: ${buildResult.success}, Длительность: $duration" }

            return buildResult.copy(duration = duration)

        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now())
            logger.error(e) { "Сборка завершилась с ошибкой после $duration" }
            throw BuildException("Сборка завершилась с ошибкой: ${e.message}", e)
        }
    }

    override suspend fun buildConfiguration(projectProperties: ApplicationProperties): BuildResult {
        val startTime = Instant.now()
        logger.info { "Собираю конфигурацию для проекта: ${projectProperties.basePath}" }

        return withContext(Dispatchers.IO) {
            try {
                val configResult = executeConfigurationBuildDsl(projectProperties)
                val duration = Duration.between(startTime, Instant.now())

                logger.info { "Сборка конфигурации завершена. Успех: ${configResult.success}, Длительность: $duration" }

                configResult.copy(duration = duration)

            } catch (e: Exception) {
                val duration = Duration.between(startTime, Instant.now())
                logger.error(e) { "Сборка конфигурации завершилась с ошибкой после $duration" }
                throw BuildException("Сборка конфигурации завершилась с ошибкой: ${e.message}", e)
            }
        }
    }

    override suspend fun buildExtension(name: String, projectProperties: ApplicationProperties): BuildResult {
        val startTime = Instant.now()
        logger.info { "Собираю расширение: $name" }

        return withContext(Dispatchers.IO) {
            try {
                val extensionResult = executeExtensionBuildDsl(name, projectProperties)
                val duration = Duration.between(startTime, Instant.now())

                logger.info { "Сборка расширения завершена: $name. Успех: ${extensionResult.success}, Длительность: $duration" }

                extensionResult.copy(duration = duration)

            } catch (e: Exception) {
                val duration = Duration.between(startTime, Instant.now())
                logger.error(e) { "Сборка расширения завершилась с ошибкой: $name после $duration" }
                throw BuildException("Сборка расширения завершилась с ошибкой для $name: ${e.message}", e)
            }
        }
    }

    /**
     * Выполняет единый DSL для полной сборки проекта
     */
    private fun executeBuildDsl(properties: ApplicationProperties): BuildResult {
        logger.info { "Формирую единый DSL для сборки проекта" }

        val typeFilter = setOf(SourceSetType.CONFIGURATION, SourceSetType.EXTENSION)
        val purposeFilter = setOf(SourceSetPurpose.MAIN, SourceSetPurpose.YAXUNIT)
        val sourceSets = properties.sourceSet
            .filter { it.type in typeFilter && it.purpose.intersect(purposeFilter).isNotEmpty() }

        val configuration = getMainConfiguration(sourceSets)!!
        val extensions = sourceSets.filter { it.type == SourceSetType.EXTENSION }

        val results = mutableMapOf<String, ConfiguratorResult>()
        sourceSets.associateTo(results) { it.name to ConfiguratorResult.EMPTY }

        dsl.configurator(properties.platformVersion) {
            // Подключаемся к информационной базе
            connect(properties.connection.connectionString)

            // Отключаем диалоги и сообщения для автоматической работы
            disableStartupDialogs()
            disableStartupMessages()

            // Загружаем основную конфигурацию
            logger.info { "Загружаю основную конфигурацию" }
            results[configuration.name] = loadConfigFromFiles {
                fromPath(properties.basePath.resolve(configuration.path))
                updateConfigDumpInfo()
            }

            // Загружаем расширения
            extensions.forEach {
                results[it.name] = loadConfigFromFiles {
                    fromPath(properties.basePath.resolve(it.path))
                    extension(it.name)
                    updateConfigDumpInfo()
                }
            }
//            val extensions = getExtensionsFromProject(properties)
//            logger.info { "Загружаю ${extensions.size} расширений: ${extensions.joinToString(", ")}" }
//
//            val extensionResults = mutableListOf<String>()
//            extensions.forEach { extensionName ->
//                val extensionPath = getExtensionPath(properties, extensionName)
//                val loadExtensionResult = loadExtension(extensionPath)
//
//                if (loadExtensionResult.success) {
//                    extensionResults.add(extensionName)
//                    logger.info { "Расширение $extensionName загружено успешно" }
//                } else {
//                    logger.error { "Ошибка загрузки расширения $extensionName: ${loadExtensionResult.error}" }
//                }
//            }

//            // Проверяем возможность применения расширений
//            logger.info { "Проверяю возможность применения расширений" }
//            val checkExtensionsResult = checkCanApplyExtensions()
//
//            if (!checkExtensionsResult.success) {
//                logger.error { "Ошибка проверки расширений: ${checkExtensionsResult.error}" }
//                return BuildResult(
//                    success = false,
//                    extensionsBuilt = extensionResults,
//                    errors = listOf("Ошибка проверки расширений: ${checkExtensionsResult.error}")
//                )
//            }
//
//            // Обновляем конфигурацию в базе данных
//            logger.info { "Обновляю конфигурацию в базе данных" }
//            val updateResult = updateDatabaseConfig()
//
//            if (!updateResult.success) {
//                logger.error { "Ошибка обновления конфигурации: ${updateResult.error}" }
//                return BuildResult(
//                    success = false,
//                    extensionsBuilt = extensionResults,
//                    errors = listOf("Ошибка обновления конфигурации: ${updateResult.error}")
//                )
//            }
//
//            // Проверяем конфигурацию
//            logger.info { "Проверяю конфигурацию" }
//            val checkConfigResult = checkConfig()
//
//            if (!checkConfigResult.success) {
//                logger.error { "Ошибка проверки конфигурации: ${checkConfigResult.error}" }
//                return BuildResult(
//                    success = false,
//                    configurationBuilt = true,
//                    extensionsBuilt = extensionResults,
//                    errors = listOf("Ошибка проверки конфигурации: ${checkConfigResult.error}")
//                )
//            }
//
//            // Проверяем модули
//            logger.info { "Проверяю модули конфигурации" }
//            val checkModulesResult = checkModules()
//
//            if (!checkModulesResult.success) {
//                logger.error { "Ошибка проверки модулей: ${checkModulesResult.error}" }
//                return BuildResult(
//                    success = false,
//                    configurationBuilt = true,
//                    extensionsBuilt = extensionResults,
//                    errors = listOf("Ошибка проверки модулей: ${checkModulesResult.error}")
//                )
//            }
//
//            logger.info { "Сборка завершена успешно" }
//            return BuildResult(
//                success = true,
//                configurationBuilt = true,
//                extensionsBuilt = extensionResults
//            )
        }

        // Если DSL не вернул результат, возвращаем ошибку
        return BuildResult(
            success = false,
            errors = listOf("DSL сборки не вернул результат")
        )
    }

    /**
     * Выполняет DSL для сборки только конфигурации
     */
    private fun executeConfigurationBuildDsl(projectProperties: ApplicationProperties): BuildResult {
        logger.info { "Формирую DSL для сборки конфигурации" }

        dsl.configurator {
            // Подключаемся к информационной базе
            connect(projectProperties.connection.connectionString)

            // Отключаем диалоги и сообщения для автоматической работы
            disableStartupDialogs()
            disableStartupMessages()

            // Загружаем основную конфигурацию
            logger.info { "Загружаю основную конфигурацию" }
            val mainConfigPath = getMainConfiguration(projectProperties.sourceSet)
            if (mainConfigPath != null) {
                loadConfigFromFiles {
                    fromPath(projectProperties.basePath.resolve(mainConfigPath.path))
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
            errors = listOf("DSL сборки конфигурации не вернул результат")
        )
    }

    /**
     * Выполняет DSL для сборки расширения
     */
    private suspend fun executeExtensionBuildDsl(
        extensionName: String,
        projectProperties: ApplicationProperties
    ): BuildResult {
        logger.info { "Формирую DSL для сборки расширения: $extensionName" }

        dsl.configurator {
            // Подключаемся к информационной базе
            connect(projectProperties.connection.connectionString)

            // Отключаем диалоги и сообщения для автоматической работы
            disableStartupDialogs()
            disableStartupMessages()

            // Загружаем расширение
            logger.info { "Загружаю расширение: $extensionName" }
            val extensionPath = getExtensionPath(projectProperties, extensionName)
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
            errors = listOf("DSL сборки расширения не вернул результат")
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
    private fun getExtensionPath(projectProperties: ApplicationProperties, extensionName: String): Path {
        val extension = projectProperties.sourceSet
            .find { it.type == SourceSetType.EXTENSION && it.path.contains(extensionName) }

        return extension?.let { projectProperties.basePath.resolve(it.path) }
            ?: projectProperties.basePath.resolve("Extensions").resolve("$extensionName.cf")
    }
} 