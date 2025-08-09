package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import io.github.alkoleft.mcp.core.modules.UtilityLocation
import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

/**
 * Контекст для работы с утилитами платформы 1С
 */
@Component
class PlatformUtilityContext(
    private val utilLocator: UtilityLocator,
    val version: String?,
) {
    private var lastError: String? = null
    private var lastOutput: String = ""
    private var lastExitCode: Int = 0
    private var lastDuration: kotlin.time.Duration = kotlin.time.Duration.ZERO

    /**
     * Получает локацию утилиты указанного типа
     */
    suspend fun locateUtility(utilityType: UtilityType): UtilityLocation = utilLocator.locateUtility(utilityType, version)

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
        } catch (e: Exception) {
            false
        }

    /**
     * Синхронная версия проверки доступности утилиты
     */
    fun isUtilityAvailableSync(utilityType: UtilityType): Boolean =
        runBlocking {
            isUtilityAvailable(utilityType)
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
