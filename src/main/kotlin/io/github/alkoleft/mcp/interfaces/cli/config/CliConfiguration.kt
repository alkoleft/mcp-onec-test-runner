package io.github.alkoleft.mcp.interfaces.cli.config

import java.nio.file.Path

/**
 * Configuration holder for CLI parameters and environment variables.
 * Consolidates all configuration sources for the application.
 */
data class CliConfiguration(
    val projectPath: Path,
    val testsPath: Path,
    val ibConnection: String,
    val ibUser: String? = null,
    val ibPassword: String? = null,
    val platformVersion: String? = null,
    val logFile: Path? = null,
    val configFile: Path? = null,
) {
    /**
     * Gets IB user from CLI parameter or environment variable
     */
    fun resolveIbUser(): String? = ibUser ?: System.getenv("IB_USER")

    /**
     * Gets IB password from CLI parameter or environment variable
     */
    fun resolveIbPassword(): String? = ibPassword ?: System.getenv("IB_PWD")

    /**
     * Validates required configuration parameters
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (!java.nio.file.Files
                .exists(projectPath)
        ) {
            errors.add("Project path does not exist: $projectPath")
        }

        if (ibConnection.isBlank()) {
            errors.add("IB connection string is required")
        }

        return errors
    }

    /**
     * Creates a summary of the configuration for logging
     */
    fun summary(): String =
        """
            |Project Path: $projectPath
            |Tests Path: $testsPath
            |IB Connection: ${ibConnection.take(20)}...
            |Platform Version: ${platformVersion ?: "auto-detect"}
            |Log File: ${logFile?.toString() ?: "console"}
            |Config File: ${configFile?.toString() ?: "none"}
        """.trimMargin()
}
