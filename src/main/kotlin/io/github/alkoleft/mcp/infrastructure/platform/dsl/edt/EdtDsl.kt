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

package io.github.alkoleft.mcp.infrastructure.platform.dsl.edt

import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilities
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.measureTime

private val logger = KotlinLogging.logger {}

/**
 * DSL for 1C:EDT CLI commands with immediate execution.
 *
 * EDT CLI provides the following main commands according to official documentation:
 * https://its.1c.ru/db/edtdoc#content:10608:hdoc
 *
 * Main commands:
 * - build: очистка и пересборка проектов
 * - cd: отображение или изменение текущего рабочего каталога
 * - clean-up-source: оптимизация формата хранения данных проекта
 * - delete: удаление проектов
 * - exit: выход из интерактивного режима
 * - export: экспорт проекта в .xml-файлы конфигурации
 * - format-modules: форматирование модулей встроенного языка
 * - help: справка по командам
 * - import: импорт проектов и конфигураций
 * - infobase: работа с информационными базами
 * - project: информация о проектах
 * - script: работа со скриптами
 * - sort-project: сортировка объектов конфигурации
 * - validate: проверка проекта
 * - version: версия EDT и компонентов
 */
class EdtDsl(
    private val utilityContext: PlatformUtilities,
) {
    private val context = EdtContext(utilityContext)

    /**
     * Executes EDT CLI command to print version
     */
    fun version(): EdtResult = executeEdt(listOf("version"))

    /**
     * Runs an arbitrary EDT CLI command with provided arguments.
     * Example: edt { run("workspace", "--list") }
     */
    fun run(vararg args: String): EdtResult = executeEdt(args.toList())

    // Build commands

    /**
     * Build project(s). Cleans and rebuilds all or some projects.
     */
    fun build(
        projects: List<String>? = null,
        yes: Boolean = true,
    ): EdtResult {
        val args = mutableListOf("build")
        if (yes) args.add("--yes")
        projects?.let { args.addAll(it) }
        return executeEdt(args)
    }

    /**
     * Build specific projects
     */
    fun buildProjects(
        vararg projectNames: String,
        yes: Boolean = true,
    ): EdtResult = build(projectNames.toList(), yes)

    // Directory commands

    /**
     * Show current working directory or change it
     */
    fun cd(directory: String? = null): EdtResult =
        if (directory != null) {
            executeEdt(listOf("cd", directory))
        } else {
            executeEdt(listOf("cd"))
        }

    // Clean-up commands

    /**
     * Optimize project data storage format
     */
    fun cleanUpSource(
        projectPath: String? = null,
        projectName: String? = null,
        includeFullSupportObjects: Boolean = false,
    ): EdtResult {
        val args = mutableListOf("clean-up-source")
        when {
            projectPath != null -> args.addAll(listOf("--project", projectPath))
            projectName != null -> args.addAll(listOf("--project-name", projectName))
            else -> throw IllegalArgumentException("Должен быть указан либо projectPath, либо projectName")
        }
        if (includeFullSupportObjects) {
            args.addAll(listOf("--include-full-support-objects", "true"))
        }
        return executeEdt(args)
    }

    // Delete commands

    /**
     * Delete projects
     */
    fun delete(
        projects: List<String>? = null,
        yes: Boolean = true,
    ): EdtResult {
        val args = mutableListOf("delete")
        if (yes) args.add("--yes")
        projects?.let { args.addAll(it) }
        return executeEdt(args)
    }

    /**
     * Delete specific projects
     */
    fun deleteProjects(
        vararg projectNames: String,
        yes: Boolean = true,
    ): EdtResult = delete(projectNames.toList(), yes)

    // Export commands

    /**
     * Export project to XML configuration files
     */
    fun export(
        projectPath: String? = null,
        projectName: String? = null,
        configurationFiles: Path,
    ): EdtResult {
        logger.info { "Экспорт проекта $projectName в каталог $configurationFiles" }
        val args = mutableListOf("export")
        when {
            projectPath != null -> args.addAll(listOf("--project", "\"$projectPath\""))
            projectName != null -> args.addAll(listOf("--project-name", "\"$projectName\""))
            else -> throw IllegalArgumentException("Должен быть указан либо projectPath, либо projectName")
        }
        args.addAll(listOf("--configuration-files", "\"$configurationFiles\""))
        return executeEdt(args)
    }

    // Format commands

    /**
     * Format modules in project
     */
    fun formatModules(
        projectPath: String? = null,
        projectName: String? = null,
    ): EdtResult {
        val args = mutableListOf("format-modules")
        when {
            projectPath != null -> args.addAll(listOf("--project", projectPath))
            projectName != null -> args.addAll(listOf("--project-name", projectName))
            else -> throw IllegalArgumentException("Должен быть указан либо projectPath, либо projectName")
        }
        return executeEdt(args)
    }

    // Import commands

    /**
     * Import project into workspace
     */
    fun importProject(projectPath: String): EdtResult = executeEdt(listOf("import", "--project", projectPath))

    /**
     * Import XML configuration files into project
     */
    fun importConfiguration(
        configurationFiles: String,
        projectPath: String? = null,
        projectName: String? = null,
        version: String? = null,
        baseProjectName: String? = null,
        build: Boolean = false,
    ): EdtResult {
        val args = mutableListOf("import", "--configuration-files", configurationFiles)
        when {
            projectPath != null -> args.addAll(listOf("--project", projectPath))
            projectName != null -> args.addAll(listOf("--project-name", projectName))
            else -> throw IllegalArgumentException("Должен быть указан либо projectPath, либо projectName")
        }
        version?.let { args.addAll(listOf("--version", it)) }
        baseProjectName?.let { args.addAll(listOf("--base-project-name", it)) }
        if (build) args.addAll(listOf("--build", "true"))
        return executeEdt(args)
    }

    // Infobase commands

    /**
     * Show infobase list
     */
    fun infobase(
        details: Boolean = false,
        infobases: List<String>? = null,
    ): EdtResult {
        val args = mutableListOf("infobase")
        if (details) args.add("--details")
        infobases?.let { args.addAll(it) }
        return executeEdt(args)
    }

    /**
     * Create infobase
     */
    fun infobaseCreate(
        name: String,
        version: String? = null,
        path: String? = null,
        configurationFile: String? = null,
    ): EdtResult {
        val args = mutableListOf("infobase-create", "--name", name)
        version?.let { args.addAll(listOf("--version", it)) }
        path?.let { args.addAll(listOf("--path", it)) }
        configurationFile?.let { args.addAll(listOf("--cf", it)) }
        return executeEdt(args)
    }

    /**
     * Delete infobase(s)
     */
    fun infobaseDelete(
        names: List<String>? = null,
        name: String? = null,
        yes: Boolean = false,
        deleteContent: Boolean = false,
    ): EdtResult {
        val args = mutableListOf("infobase-delete")
        when {
            names != null -> args.addAll(names)
            name != null -> args.addAll(listOf("--name", name))
            else -> throw IllegalArgumentException("Должен быть указан либо names, либо name")
        }
        if (yes) args.addAll(listOf("--yes", "true"))
        if (deleteContent) args.addAll(listOf("--delete-content", "true"))
        return executeEdt(args)
    }

    /**
     * Import infobase into project
     */
    fun infobaseImport(
        name: String,
        project: String,
        build: Boolean = false,
    ): EdtResult {
        val args = mutableListOf("infobase-import", "--name", name, "--project", project)
        if (build) args.addAll(listOf("--build", "true"))
        return executeEdt(args)
    }

    // Platform support commands

    /**
     * Install platform support
     */
    fun installPlatformSupport(version: String): EdtResult = executeEdt(listOf("install-platform-support", "--version", version))

    /**
     * Uninstall platform support
     */
    fun uninstallPlatformSupport(version: String): EdtResult = executeEdt(listOf("uninstall-platform-support", "--version", version))

    /**
     * Show platform versions
     */
    fun platformVersions(): EdtResult = executeEdt(listOf("platform-versions"))

    // Project commands

    /**
     * Show project information
     */
    fun project(
        details: Boolean = false,
        projects: List<String>? = null,
    ): EdtResult {
        val args = mutableListOf("project")
        if (details) args.add("--details")
        projects?.let { args.addAll(it) }
        return executeEdt(args)
    }

    // Script commands

    /**
     * Show available scripts
     */
    fun script(): EdtResult = executeEdt(listOf("script"))

    /**
     * Show script information
     */
    fun scriptInfo(
        scriptName: String,
        content: Boolean = false,
    ): EdtResult {
        val args = mutableListOf("script", scriptName)
        if (content) args.add("--content")
        return executeEdt(args)
    }

    /**
     * Load script
     */
    fun scriptLoad(
        scriptPath: String,
        recursive: Boolean = true,
        namespace: String? = null,
    ): EdtResult {
        val args = mutableListOf("script", "--load", scriptPath)
        if (recursive) args.addAll(listOf("--recursive", "true"))
        namespace?.let { args.addAll(listOf("--namespace", it)) }
        return executeEdt(args)
    }

    // Sort project commands

    /**
     * Sort project objects
     */
    fun sortProject(
        projectPaths: List<String>? = null,
        projectNames: List<String>? = null,
    ): EdtResult {
        val args = mutableListOf("sort-project")
        when {
            projectPaths != null -> args.addAll(listOf("--project-list") + projectPaths)
            projectNames != null -> args.addAll(listOf("--project-name-list") + projectNames)
            else -> throw IllegalArgumentException("Должен быть указан либо projectPaths, либо projectNames")
        }
        return executeEdt(args)
    }

    // Validate commands

    /**
     * Validate project
     */
    fun validate(
        outputFile: String,
        projectPaths: List<String>? = null,
        projectNames: List<String>? = null,
    ): EdtResult {
        val args = mutableListOf("validate", "--file", outputFile)
        when {
            projectPaths != null -> args.addAll(listOf("--project-list") + projectPaths)
            projectNames != null -> args.addAll(listOf("--project-name-list") + projectNames)
            else -> throw IllegalArgumentException("Должен быть указан либо projectPaths, либо projectNames")
        }
        return executeEdt(args)
    }

    /**
     * Executes an EDT CLI command with specified arguments immediately.
     */
    private fun executeEdt(arguments: List<String>): EdtResult {
        logger.debug { "Запуск 1C:EDT: args=${arguments.joinToString(" ")}" }
        val duration =
            measureTime {
                try {
                    val executor = utilityContext.executor(UtilityType.EDT_CLI)
                    val result = executor.execute(arguments)
                    logger.info { "Команда 1C:EDT выполнен: exitCode=${result.exitCode}, длительность=${result.duration}" }
                    if (result.exitCode != 0) {
                        val errorPreview: String? =
                            result.error?.let { if (it.length > 4000) it.take(4000) + "..." else it }
                        logger.warn { "Команда 1C:EDT завершилась с ошибкой: exitCode=${result.exitCode}, ошибка=$errorPreview" }
                    }
                    context.setResult(
                        success = result.exitCode == 0,
                        output = result.output,
                        error = result.error,
                        exitCode = result.exitCode,
                        duration = result.duration,
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Команда 1C:EDT выбросила исключение для args=${arguments.joinToString(" ")}" }
                    context.setResult(
                        success = false,
                        output = "",
                        error = e.message ?: "Неизвестная ошибка",
                        exitCode = -1,
                        duration = Duration.ZERO,
                    )
                }
            }

        logger.debug { "Команда 1C:EDT общая длительность=$duration" }
        return EdtResult(
            success = context.buildResult().success,
            output = context.buildResult().output,
            error = context.buildResult().error,
            exitCode = context.buildResult().exitCode,
            duration = duration,
        )
    }
}

/**
 * Context for EDT CLI.
 */
class EdtContext(
    val platformContext: PlatformUtilities,
) {
    private var lastError: String? = null
    private var lastOutput: String = ""
    private var lastExitCode: Int = 0
    private var lastDuration: Duration = Duration.ZERO

    /**
     * Builds argument list for EDT CLI.
     * EDT does not use Designer/Enterprise modes, so no mode token is added.
     */
    fun buildEdtArgs(commandArgs: List<String>): List<String> {
        val args = mutableListOf<String>()
        val location = platformContext.locateUtility(UtilityType.EDT_CLI).executablePath
        args.add(location.toString())
        args.addAll(commandArgs)
        return args
    }

    /**
     * Sets the result of execution
     */
    fun setResult(
        success: Boolean,
        output: String,
        error: String?,
        exitCode: Int,
        duration: kotlin.time.Duration,
    ) {
        this.lastOutput = output
        this.lastError = error
        this.lastExitCode = exitCode
        this.lastDuration = duration
    }

    /**
     * Builds the result of execution
     */
    fun buildResult(): EdtResult =
        EdtResult(
            success = lastExitCode == 0,
            output = lastOutput,
            error = lastError,
            exitCode = lastExitCode,
            duration = lastDuration,
        )
}

/**
 * Result of EDT CLI execution
 */
data class EdtResult(
    override val success: Boolean,
    override val output: String,
    override val error: String?,
    override val exitCode: Int,
    override val duration: Duration,
) : ShellCommandResult {
    companion object {
        val EMPTY = EdtResult(false, "", "", -1, Duration.ZERO)
    }
}
