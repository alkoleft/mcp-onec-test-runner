package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.ConfiguratorResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

/**
 * Реализация BuildAction для сборки через 1C:EDT CLI
 */
@Component
@ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "EDT")
class EdtBuildAction(
    dsl: PlatformDsl,
) : AbstractBuildAction(dsl) {
    /**
     * Выполняет единый DSL для полной сборки проекта через EDT
     */
    override suspend fun executeBuildDsl(
        properties: ApplicationProperties,
        sourceSet: SourceSet,
    ): BuildResult {
        logger.debug { "Формирую единый DSL для сборки проекта через EDT" }

        val results = mutableMapOf<String, ConfiguratorResult>()
        sourceSet.associateTo(results) { it.name to ConfiguratorResult.EMPTY }

        var buildResult: BuildResult? = null

        dsl.edt {
            // Проверяем версию EDT
            val versionResult = version()
            if (!versionResult.success) {
                logger.error { "Не удалось получить версию EDT: ${versionResult.error}" }
                return@edt
            }
            logger.info { "EDT версия: ${versionResult.output}" }

            // Переходим в базовую директорию проекта
            val cdResult = cd(properties.basePath.toString())
            if (!cdResult.success) {
                logger.error { "Не удалось перейти в директорию проекта: ${cdResult.error}" }
                return@edt
            }

            // Собираем основную конфигурацию
            sourceSet.configuration?.also { configuration ->
                logger.info { "Собираю основную конфигурацию: ${configuration.name}" }
                val configPath = properties.basePath.resolve(configuration.path)

                // Переходим в директорию конфигурации
                val configCdResult = cd(configPath.toString())
                if (!configCdResult.success) {
                    logger.error { "Не удалось перейти в директорию конфигурации: ${configCdResult.error}" }
                    return@edt
                }

                // Выполняем сборку конфигурации
                val buildResult = build(yes = true)
                if (buildResult.success) {
                    logger.info { "Конфигурация ${configuration.name} собрана успешно" }
                    results[configuration.name] =
                        ConfiguratorResult(
                            success = true,
                            output = buildResult.output,
                            error = buildResult.error,
                            exitCode = buildResult.exitCode,
                            duration = buildResult.duration,
                        )
                } else {
                    logger.error { "Ошибка сборки конфигурации ${configuration.name}: ${buildResult.error}" }
                    results[configuration.name] =
                        ConfiguratorResult(
                            success = false,
                            output = buildResult.output,
                            error = buildResult.error,
                            exitCode = buildResult.exitCode,
                            duration = buildResult.duration,
                        )
                }
            }

            // Собираем расширения
            val extensions = sourceSet.extensions
            logger.info { "Собираю ${extensions.size} расширений: ${extensions.joinToString(", ")}" }

            extensions.forEach { extension ->
                logger.info { "Собираю расширение: ${extension.name}" }
                val extensionPath = properties.basePath.resolve(extension.path)

                // Переходим в директорию расширения
                val extCdResult = cd(extensionPath.toString())
                if (!extCdResult.success) {
                    logger.error { "Не удалось перейти в директорию расширения ${extension.name}: ${extCdResult.error}" }
                    results[extension.name] =
                        ConfiguratorResult(
                            success = false,
                            output = extCdResult.output,
                            error = extCdResult.error,
                            exitCode = extCdResult.exitCode,
                            duration = extCdResult.duration,
                        )
                    return@forEach
                }

                // Выполняем сборку расширения
                val extBuildResult = build(yes = true)
                if (extBuildResult.success) {
                    logger.info { "Расширение ${extension.name} собрано успешно" }
                    results[extension.name] =
                        ConfiguratorResult(
                            success = true,
                            output = extBuildResult.output,
                            error = extBuildResult.error,
                            exitCode = extBuildResult.exitCode,
                            duration = extBuildResult.duration,
                        )
                } else {
                    logger.error { "Ошибка сборки расширения ${extension.name}: ${extBuildResult.error}" }
                    results[extension.name] =
                        ConfiguratorResult(
                            success = false,
                            output = extBuildResult.output,
                            error = extBuildResult.error,
                            exitCode = extBuildResult.exitCode,
                            duration = extBuildResult.duration,
                        )
                }
            }

            // Проверяем общий результат сборки
            val hasErrors = results.values.any { !it.success }
            if (hasErrors) {
                val errorMessages =
                    results.values
                        .filter { !it.success }
                        .mapNotNull { it.error }

                logger.error { "Сборка завершилась с ошибками: ${errorMessages.joinToString("; ")}" }
                buildResult =
                    BuildResult(
                        success = false,
                        sourceSet = results.toMap(),
                        errors = errorMessages,
                    )
            } else {
                logger.info { "Сборка завершена успешно" }
                buildResult =
                    BuildResult(
                        success = true,
                        sourceSet = results.toMap(),
                    )
            }
        }

        // Если DSL не вернул результат, возвращаем ошибку
        return buildResult ?: BuildResult(
            success = false,
            sourceSet = results.toMap(),
            errors = listOf("EDT DSL не вернул результат сборки"),
        )
    }
}
