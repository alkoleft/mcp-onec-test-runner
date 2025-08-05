package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.application.actions.BuildAction
import io.github.alkoleft.mcp.application.actions.BuildResult
import io.github.alkoleft.mcp.application.actions.exceptions.BuildException
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.SourceSetType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

/**
 * Реализация BuildAction для сборки через ibcmd
 */
class IbcmdBuildAction(
    private val platformUtilityDsl: PlatformUtilityDsl
) : BuildAction {

    override suspend fun build(projectProperties: ApplicationProperties): BuildResult {
        val startTime = Instant.now()
        logger.info { "Starting full build via ibcmd for project: ${projectProperties.basePath}" }

        try {
            // Сначала собираем конфигурацию
            val configResult = buildConfiguration(projectProperties)
            if (!configResult.success) {
                return configResult.copy(duration = Duration.between(startTime, Instant.now()))
            }

            // Затем собираем все расширения
            val extensionResults = mutableListOf<BuildResult>()
            val extensions = getExtensionsFromProject(projectProperties)

            logger.info { "Building ${extensions.size} extensions via ibcmd: ${extensions.joinToString(", ")}" }

            extensions.forEach { extensionName ->
                val result = buildExtension(extensionName, projectProperties)
                extensionResults.add(result)
            }

            val duration = Duration.between(startTime, Instant.now())
            val success = configResult.success && extensionResults.all { it.success }
            val allErrors = configResult.errors + extensionResults.flatMap { it.errors }

            logger.info { "Ibcmd build completed. Success: $success, Duration: $duration" }

            return BuildResult(
                success = success,
                configurationBuilt = configResult.success,
                extensionsBuilt = extensionResults.filter { it.success }.flatMap { it.extensionsBuilt },
                errors = allErrors,
                duration = duration
            )

        } catch (e: Exception) {
            val duration = Duration.between(startTime, Instant.now())
            logger.error(e) { "Ibcmd build failed after $duration" }
            throw BuildException("Ibcmd build failed: ${e.message}", e)
        }
    }

    override suspend fun buildConfiguration(projectProperties: ApplicationProperties): BuildResult {
        val startTime = Instant.now()
        logger.info { "Building configuration via ibcmd for project: ${projectProperties.basePath}" }

        return withContext(Dispatchers.IO) {
            try {
                // TODO: Implement actual ibcmd configuration build logic
                // For now, return a successful mock result
                val duration = Duration.between(startTime, Instant.now())

                logger.info { "Configuration ibcmd build completed (mock). Success: true, Duration: $duration" }

                BuildResult(
                    success = true,
                    configurationBuilt = true,
                    errors = emptyList(),
                    duration = duration
                )

            } catch (e: Exception) {
                val duration = Duration.between(startTime, Instant.now())
                logger.error(e) { "Configuration ibcmd build failed after $duration" }
                throw BuildException("Configuration ibcmd build failed: ${e.message}", e)
            }
        }
    }

    override suspend fun buildExtension(name: String, projectProperties: ApplicationProperties): BuildResult {
        val startTime = Instant.now()
        logger.info { "Building extension via ibcmd: $name" }

        return withContext(Dispatchers.IO) {
            try {
                // TODO: Implement actual ibcmd extension build logic
                // For now, return a successful mock result
                val duration = Duration.between(startTime, Instant.now())

                logger.info { "Extension ibcmd build completed (mock): $name. Success: true, Duration: $duration" }

                BuildResult(
                    success = true,
                    extensionsBuilt = listOf(name),
                    errors = emptyList(),
                    duration = duration
                )

            } catch (e: Exception) {
                val duration = Duration.between(startTime, Instant.now())
                logger.error(e) { "Extension ibcmd build failed: $name after $duration" }
                throw BuildException("Extension ibcmd build failed for $name: ${e.message}", e)
            }
        }
    }

    private fun getExtensionsFromProject(projectProperties: ApplicationProperties): List<String> {
        return projectProperties.sourceSet
            .filter { it.type == SourceSetType.EXTENSION }
            .map { it.path }
    }
} 