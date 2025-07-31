package io.github.alkoleft.mcp.infrastructure.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger { }

/**
 * Project configuration management for MCP YaXUnit Runner.
 * Handles configuration loading from files, environment variables, and defaults.
 */
@Component
class ProjectConfigurationManager {
    private val jsonMapper = jacksonObjectMapper()
    private val yamlMapper =
        ObjectMapper(YAMLFactory()).apply {
            findAndRegisterModules()
        }

    /**
     * Loads configuration from file, environment variables, and defaults
     */
    fun loadConfiguration(configFilePath: Path? = null): ProjectConfiguration {
        val defaultConfig = createDefaultConfiguration()
        val envConfig = loadEnvironmentConfiguration()
        val fileConfig = configFilePath?.let { loadFileConfiguration(it) }

        // Merge configurations: defaults < environment < file
        return mergeConfigurations(defaultConfig, envConfig, fileConfig)
    }

    /**
     * Creates default configuration
     */
    private fun createDefaultConfiguration(): ProjectConfiguration =
        ProjectConfiguration(
            project =
                ProjectSettings(
                    testsPath = "./tests",
                    buildTimeout = 300000, // 5 minutes
                    testTimeout = 600000, // 10 minutes
                ),
            platform =
                PlatformSettings(
                    autoDetectVersion = true,
                    searchPaths = getDefaultPlatformPaths(),
                ),
            build =
                BuildSettings(
                    incrementalEnabled = true,
                    hashStorageSize = 10000,
                    parallelBuild = true,
                ),
            logging =
                LoggingSettings(
                    level = "INFO",
                    enableFileLogging = false,
                ),
            server =
                ServerSettings(
                    port = 8080,
                    enableWebSocket = true,
                    webSocketPath = "/yaxunit",
                ),
        )

    /**
     * Loads configuration from environment variables
     */
    private fun loadEnvironmentConfiguration(): ProjectConfiguration =
        ProjectConfiguration(
            project =
                ProjectSettings(
                    testsPath = System.getenv("YAXUNIT_TESTS_PATH") ?: "./tests",
                    buildTimeout = System.getenv("YAXUNIT_BUILD_TIMEOUT")?.toLongOrNull() ?: 300000,
                    testTimeout = System.getenv("YAXUNIT_TEST_TIMEOUT")?.toLongOrNull() ?: 600000,
                ),
            platform =
                PlatformSettings(
                    version = System.getenv("YAXUNIT_PLATFORM_VERSION"),
                    autoDetectVersion = System.getenv("YAXUNIT_AUTO_DETECT")?.toBoolean() ?: true,
                    searchPaths =
                        System
                            .getenv("YAXUNIT_PLATFORM_PATHS")
                            ?.split(System.getProperty("path.separator"))
                            ?.map { Paths.get(it) }
                            ?: getDefaultPlatformPaths(),
                ),
            informationBase =
                InformationBaseSettings(
                    connection = System.getenv("IB_CONNECTION"),
                    user = System.getenv("IB_USER"),
                    password = System.getenv("IB_PWD"),
                ),
            build =
                BuildSettings(
                    incrementalEnabled = System.getenv("YAXUNIT_INCREMENTAL_BUILD")?.toBoolean() ?: true,
                    hashStorageSize = System.getenv("YAXUNIT_HASH_STORAGE_SIZE")?.toIntOrNull() ?: 10000,
                    parallelBuild = System.getenv("YAXUNIT_PARALLEL_BUILD")?.toBoolean() ?: true,
                ),
            logging =
                LoggingSettings(
                    level = System.getenv("YAXUNIT_LOG_LEVEL") ?: "INFO",
                    enableFileLogging = System.getenv("YAXUNIT_FILE_LOGGING")?.toBoolean() ?: false,
                    filePath = System.getenv("YAXUNIT_LOG_FILE")?.let { Paths.get(it) },
                ),
            server =
                ServerSettings(
                    port = System.getenv("YAXUNIT_SERVER_PORT")?.toIntOrNull() ?: 8080,
                    enableWebSocket = System.getenv("YAXUNIT_WEBSOCKET_ENABLED")?.toBoolean() ?: true,
                    webSocketPath = System.getenv("YAXUNIT_WEBSOCKET_PATH") ?: "/yaxunit",
                ),
        )

    /**
     * Loads configuration from YAML or JSON file
     */
    private fun loadFileConfiguration(configPath: Path): ProjectConfiguration? {
        return try {
            if (!Files.exists(configPath)) {
                logger.warn { "Configuration file not found: $configPath" }
                return null
            }

            val content = Files.readString(configPath)
            val mapper =
                when (configPath.toString().lowercase()) {
                    in listOf("yml", "yaml") -> yamlMapper
                    else -> jsonMapper
                }

            val config = mapper.readValue<ProjectConfiguration>(content)
            logger.info { "Loaded configuration from: $configPath" }
            config
        } catch (e: Exception) {
            logger.error(e) { "Failed to load configuration from file: $configPath" }
            null
        }
    }

    /**
     * Merges multiple configurations with precedence
     */
    private fun mergeConfigurations(
        default: ProjectConfiguration,
        environment: ProjectConfiguration? = null,
        file: ProjectConfiguration? = null,
    ): ProjectConfiguration {
        var merged = default

        environment?.let { env ->
            merged =
                merged.copy(
                    project = mergeProjectSettings(merged.project, env.project),
                    platform = mergePlatformSettings(merged.platform, env.platform),
                    informationBase = mergeInformationBaseSettings(merged.informationBase, env.informationBase),
                    build = mergeBuildSettings(merged.build, env.build),
                    logging = mergeLoggingSettings(merged.logging, env.logging),
                    server = mergeServerSettings(merged.server, env.server),
                )
        }

        file?.let { fileConfig ->
            merged =
                merged.copy(
                    project = mergeProjectSettings(merged.project, fileConfig.project),
                    platform = mergePlatformSettings(merged.platform, fileConfig.platform),
                    informationBase = mergeInformationBaseSettings(merged.informationBase, fileConfig.informationBase),
                    build = mergeBuildSettings(merged.build, fileConfig.build),
                    logging = mergeLoggingSettings(merged.logging, fileConfig.logging),
                    server = mergeServerSettings(merged.server, fileConfig.server),
                )
        }

        return merged
    }

    private fun mergeProjectSettings(
        base: ProjectSettings,
        override: ProjectSettings?,
    ): ProjectSettings =
        override?.let {
            ProjectSettings(
                testsPath = it.testsPath ?: base.testsPath,
                buildTimeout = it.buildTimeout ?: base.buildTimeout,
                testTimeout = it.testTimeout ?: base.testTimeout,
            )
        } ?: base

    private fun mergePlatformSettings(
        base: PlatformSettings,
        override: PlatformSettings?,
    ): PlatformSettings =
        override?.let {
            PlatformSettings(
                version = it.version ?: base.version,
                autoDetectVersion = it.autoDetectVersion ?: base.autoDetectVersion,
                searchPaths = it.searchPaths ?: base.searchPaths,
            )
        } ?: base

    private fun mergeInformationBaseSettings(
        base: InformationBaseSettings?,
        override: InformationBaseSettings?,
    ): InformationBaseSettings? {
        if (override == null) return base
        if (base == null) return override

        return InformationBaseSettings(
            connection = override.connection ?: base.connection,
            user = override.user ?: base.user,
            password = override.password ?: base.password,
        )
    }

    private fun mergeBuildSettings(
        base: BuildSettings,
        override: BuildSettings?,
    ): BuildSettings =
        override?.let {
            BuildSettings(
                incrementalEnabled = it.incrementalEnabled ?: base.incrementalEnabled,
                hashStorageSize = it.hashStorageSize ?: base.hashStorageSize,
                parallelBuild = it.parallelBuild ?: base.parallelBuild,
            )
        } ?: base

    private fun mergeLoggingSettings(
        base: LoggingSettings,
        override: LoggingSettings?,
    ): LoggingSettings =
        override?.let {
            LoggingSettings(
                level = it.level ?: base.level,
                enableFileLogging = it.enableFileLogging ?: base.enableFileLogging,
                filePath = it.filePath ?: base.filePath,
            )
        } ?: base

    private fun mergeServerSettings(
        base: ServerSettings,
        override: ServerSettings?,
    ): ServerSettings =
        override?.let {
            ServerSettings(
                port = it.port ?: base.port,
                enableWebSocket = it.enableWebSocket ?: base.enableWebSocket,
                webSocketPath = it.webSocketPath ?: base.webSocketPath,
            )
        } ?: base

    /**
     * Gets default platform search paths based on operating system
     */
    private fun getDefaultPlatformPaths(): List<Path> {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("windows") ->
                listOf(
                    Paths.get(System.getenv("PROGRAMFILES") ?: "C:\\Program Files", "1cv8"),
                    Paths.get(System.getenv("PROGRAMFILES(X86)") ?: "C:\\Program Files (x86)", "1cv8"),
                )

            osName.contains("linux") ->
                listOf(
                    Paths.get("/opt/1cv8"),
                    Paths.get("/usr/local/1cv8"),
                )

            else -> emptyList()
        }
    }
}

/**
 * Complete project configuration structure
 */
data class ProjectConfiguration(
    val project: ProjectSettings,
    val platform: PlatformSettings,
    val informationBase: InformationBaseSettings? = null,
    val build: BuildSettings,
    val logging: LoggingSettings,
    val server: ServerSettings,
)

data class ProjectSettings(
    @JsonProperty("tests_path")
    val testsPath: String? = null,
    @JsonProperty("build_timeout")
    val buildTimeout: Long? = null,
    @JsonProperty("test_timeout")
    val testTimeout: Long? = null,
)

data class PlatformSettings(
    val version: String? = null,
    @JsonProperty("auto_detect_version")
    val autoDetectVersion: Boolean? = null,
    @JsonProperty("search_paths")
    val searchPaths: List<Path>? = null,
)

data class InformationBaseSettings(
    val connection: String? = null,
    val user: String? = null,
    val password: String? = null,
)

data class BuildSettings(
    @JsonProperty("incremental_enabled")
    val incrementalEnabled: Boolean? = null,
    @JsonProperty("hash_storage_size")
    val hashStorageSize: Int? = null,
    @JsonProperty("parallel_build")
    val parallelBuild: Boolean? = null,
)

data class LoggingSettings(
    val level: String? = null,
    @JsonProperty("enable_file_logging")
    val enableFileLogging: Boolean? = null,
    @JsonProperty("file_path")
    val filePath: Path? = null,
)

data class ServerSettings(
    val port: Int? = null,
    @JsonProperty("enable_websocket")
    val enableWebSocket: Boolean? = null,
    @JsonProperty("websocket_path")
    val webSocketPath: String? = null,
)
