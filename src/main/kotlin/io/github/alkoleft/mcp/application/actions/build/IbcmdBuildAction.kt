package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.application.actions.exceptions.BuildError
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd.IbcmdDsl
import io.github.alkoleft.mcp.infrastructure.utility.ifNoBlank
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger { }
private val FILE_PATH_PATTERN = "File\\s*=\\s*(['\"]?)([^'\";\\n]+)\\1\\s*".toRegex()
/**
 * Реализация BuildAction для сборки через ibcmd
 */
class IbcmdBuildAction(
    dsl: PlatformDsl,
) : AbstractBuildAction(dsl) {
    private lateinit var ibcmdDsl: IbcmdDsl

    override fun initDsl(properties: ApplicationProperties) {
        ibcmdDsl = dsl.ibcmd {
            dbPath = extractFilePath(properties.connection.connectionString)
                ?: throw BuildError("Не удалось определить путь к файлу из строки подключения")
            properties.connection.user?.ifNoBlank { user(it) }
            properties.connection.password?.ifNoBlank { password(it) }
        }
    }

    override fun loadConfiguration(name: String, path: Path): BuildResult {
        lateinit var buildResult: BuildResult
        ibcmdDsl.config {
            var result = import(path)
            if (result.success) {
                logger.info { "Конфигурация загружена успешно" }
            } else {
                logger.info { "Не удалось загрузить конфигурацию" }
                buildResult = BuildResult(
                    success = false,
                    sourceSet = mapOf(name to result),
                    errors = listOf("Не удалось загрузить конфигурацию: ${result.error}"),
                )
                return@config
            }
            result = apply {
                force = true
            }
            if (result.success) {
                logger.info { "Конфигурация базы данных обновлена успешно" }
                buildResult = BuildResult(
                    success = true,
                    sourceSet = mapOf(name to result),
                )
            } else {
                logger.info { "Не удалось обновить конфигурацию базы данных" }
                buildResult = BuildResult(
                    success = false,
                    sourceSet = mapOf(name to result),
                    errors = listOf("Не удалось обновить конфигурацию базы данных: ${result.error}"),
                )
            }
        }
        return buildResult
    }

    override fun loadExtension(name: String, path: Path): BuildResult {
        lateinit var buildResult: BuildResult
        ibcmdDsl.config {
            var result = import(path) {
                extension = name
            }
            if (result.success) {
                logger.info { "Расширение $name загружено успешно" }
            } else {
                logger.info { "Не удалось загрузить расширение $name" }
                buildResult = BuildResult(
                    success = false,
                    sourceSet = mapOf(name to result),
                    errors = listOf("Не удалось загрузить расширение $name: ${result.error}"),
                )
                return@config
            }
            result = apply {
                force = true
                extension = name
            }
            if (result.success) {
                logger.info { "Конфигурация базы данных обновлена успешно (расширение $name)" }
                buildResult = BuildResult(
                    success = true,
                    sourceSet = mapOf(name to result),
                )
            } else {
                logger.info { "Не удалось обновить конфигурацию базы данных (расширение $name)" }
                buildResult = BuildResult(
                    success = false,
                    sourceSet = mapOf(name to result),
                    errors = listOf("Не удалось обновить конфигурацию базы данных (расширение $name): ${result.error}"),
                )
            }
        }
        return buildResult
    }

    private fun extractFilePath(connectionString: String) = FILE_PATH_PATTERN.find(connectionString)?.groupValues[2]
}
