package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import io.github.alkoleft.mcp.application.services.EdtCliStartService
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.edt.EdtCliExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.CommandExecutor
import io.github.alkoleft.mcp.infrastructure.platform.dsl.process.ProcessExecutor
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
    suspend fun locateUtility(utilityType: UtilityType): UtilityLocation =
        utilLocator.locateUtility(
            utilityType,
            version = if (utilityType.isPlatform()) properties.platformVersion else "latest",
        )

    /**
     * Синхронная версия получения локации утилиты
     */
    fun locateUtilitySync(utilityType: UtilityType): UtilityLocation =
        runBlocking {
            locateUtility(utilityType)
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
    fun buildResult(): PlatformUtilityResult =
        PlatformUtilityResult(
            success = lastExitCode == 0,
            output = lastOutput,
            error = lastError,
            exitCode = lastExitCode,
            duration = lastDuration,
        )

    /**
     * Проверяет, доступна ли утилита
     */
    suspend fun isUtilityAvailable(utilityType: UtilityType): Boolean =
        try {
            val location = locateUtility(utilityType)
            utilLocator.validateUtility(location)
        } catch (_: Exception) {
            false
        }

    /**
     * Получает путь к указанной утилите
     */
    fun getUtilityPath(utilityType: UtilityType): String =
        try {
            val location = locateUtilitySync(utilityType)
            location.executablePath.toString()
        } catch (e: Exception) {
            "/path/to/default/utility"
        }

    /**
     * Получает путь к утилите ibcmd
     */
    fun getUtilityPath(): String = getUtilityPath(UtilityType.IBCMD)

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

/**
 * Общий результат выполнения операций с утилитами платформы
 */
data class PlatformUtilityResult(
    val success: Boolean,
    val output: String,
    val error: String?,
    val exitCode: Int,
    val duration: kotlin.time.Duration,
)
