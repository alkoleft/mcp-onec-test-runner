package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import io.github.alkoleft.mcp.application.services.EdtCliStartService
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.edt.EdtCliExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.CommandExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessResult
import io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

/**
 * Контекст для работы с утилитами платформы 1С
 */
@Component
class PlatformUtilityContext(
    private val utilLocator: UtilityLocator,
    private val properties: ApplicationProperties,
    private val applicationContext: ApplicationContext,
) {
    private var lastError: String? = null
    private var lastOutput: String = ""
    private var lastExitCode: Int = 0
    private var lastDuration: kotlin.time.Duration = kotlin.time.Duration.ZERO

    /**
     * Получает локацию утилиты указанного типа
     */
    private suspend fun locateUtility(
        utilityType: UtilityType,
        version: String,
    ): UtilityLocation =
        utilLocator.locateUtility(
            utilityType,
            version = version,
        )

    /**
     * Синхронная версия получения локации утилиты
     */
    private fun locateUtilitySync(
        utilityType: UtilityType,
        version: String,
    ): UtilityLocation =
        runBlocking {
            locateUtility(utilityType, version)
        }

    /**
     * Устанавливает результат выполнения операции
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
     * Строит результат выполнения операций
     */
    fun buildResult(): ProcessResult =
        ProcessResult(
            success = lastExitCode == 0,
            output = lastOutput,
            error = lastError,
            exitCode = lastExitCode,
            duration = lastDuration,
        )

    /**
     * Получает путь к указанной утилите
     */
    fun getUtilityPath(
        utilityType: UtilityType,
        version: String = "default",
    ): String =
        try {
            val actualVersion: String =
                if (version == "default") {
                    if (utilityType.isPlatform()) properties.platformVersion else properties.tools.edtCli.version
                } else {
                    version
                }
            val location = locateUtilitySync(utilityType, actualVersion)
            location.executablePath.toString()
        } catch (e: Exception) {
            "/path/to/default/utility"
        }

    fun executor(utilityType: UtilityType): CommandExecutor {
        if (utilityType == UtilityType.EDT_CLI && properties.tools.edtCli.interactiveMode) {
            val service = applicationContext.getBean(EdtCliStartService::class.java)
            val executor = service.interactiveExecutor()
            return executor?.let { EdtCliExecutor(it) }
                ?: throw IllegalStateException("EDT cli не запущено, попробуйте позже")
        } else {
            return ProcessExecutor()
        }
    }
}

// Removed PlatformUtilityResult in favor of generic ProcessResult
