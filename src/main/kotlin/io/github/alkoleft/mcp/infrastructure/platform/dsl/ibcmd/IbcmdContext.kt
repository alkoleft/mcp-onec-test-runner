package io.github.alkoleft.mcp.infrastructure.platform.dsl.ibcmd

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext

/**
 * Контекст для выполнения команд ibcmd.
 * Управляет параметрами подключения и путями для команд ibcmd.
 */
class IbcmdContext(
    private val platformContext: PlatformUtilityContext,
) {
    private var dbPath: String? = null
    private var user: String? = null
    private var password: String? = null
    private var tempDataPath: String? = null // Переименовано для согласованности

    /**
     * Возвращает путь к утилите ibcmd.
     */
    val utilityPath
        get() = platformContext.locateUtilitySync(UtilityType.IBCMD).executablePath.toString()

    /**
     * Устанавливает путь к базе данных.
     *
     * @param path Путь к базе данных.
     */
    fun dbPath(path: String) {
        this.dbPath = path
    }

    /**
     * Устанавливает пользователя для подключения.
     *
     * @param user Имя пользователя.
     */
    fun user(user: String) {
        this.user = user
    }

    /**
     * Устанавливает пароль для подключения.
     *
     * @param password Пароль.
     */
    fun password(password: String) {
        this.password = password
    }

    /**
     * Устанавливает путь к каталогу временных данных сервера.
     *
     * @param path Путь к каталогу временных данных.
     */
    fun data(path: String) {
        this.tempDataPath = path
    }

    /**
     * Строит базовые аргументы для ibcmd.
     * Включает параметры подключения и пути, если они заданы.
     *
     * @return Список базовых аргументов.
     */
    fun buildBaseArgs(): List<String> {
        val args = mutableListOf<String>()

        // Путь к базе данных
        dbPath?.let {
            args.add("--db-path")
            args.add(it)
        }

        // Каталог временных данных
        tempDataPath?.let {
            args.add("--data")
            args.add(it)
        }

        // Пользователь
        user?.let {
            args.add("--user")
            args.add(it)
        }

        // Пароль
        password?.let {
            args.add("--password")
            args.add(it)
        }

        return args
    }
}
