package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext

/**
 * Контекст для выполнения команд ibcmd
 */
class IbcmdContext(
    private val platformContext: PlatformUtilityContext,
) {
    private var dbPath: String? = null
    private var user: String? = null
    private var password: String? = null

    val utilityPath
        get() = platformContext.getUtilityPath(UtilityType.IBCMD)

    /**
     * Устанавливает путь к базе данных
     */
    fun dbPath(path: String) {
        this.dbPath = path
    }

    /**
     * Устанавливает пользователя для подключения
     */
    fun user(user: String) {
        this.user = user
    }

    /**
     * Устанавливает пароль для подключения
     */
    fun password(password: String) {
        this.password = password
    }

    /**
     * Строит базовые аргументы для ibcmd
     */
    fun buildBaseArgs(): List<String> {
        val args = mutableListOf<String>()

        // Путь к базе данных
        dbPath?.let { args.addAll(listOf("--db-path", it)) }

        // Пользователь
        user?.let { args.addAll(listOf("--user", it)) }

        // Пароль
        password?.let { args.addAll(listOf("--password", it)) }

        return args
    }
}
