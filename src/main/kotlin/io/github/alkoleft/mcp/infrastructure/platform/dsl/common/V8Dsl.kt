package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

import java.nio.file.Path

/**
 * Базовый DSL класс для работы с платформой 1С
 *
 * @param T тип контекста платформы
 * @param C тип команды
 * @param context контекст платформы
 */
abstract class V8Dsl<T : V8Context, C : Command>(
    context: T,
) : Dsl<T, C>(context) {
    /**
     * Устанавливает строку подключения
     *
     * @param connectionString строка подключения к ИБ
     */
    fun connect(connectionString: String) {
        context.connect(connectionString)
    }

    /**
     * Подключается к серверу приложений
     *
     * @param serverName имя сервера
     * @param dbName имя базы данных
     */
    fun connectToServer(
        serverName: String,
        dbName: String,
    ) {
        context.connectToServer(serverName, dbName)
    }

    /**
     * Подключается к файловой БД
     *
     * @param path путь к файлу БД
     */
    fun connectToFile(path: String) {
        context.connectToFile(path)
    }

    /**
     * Устанавливает имя пользователя
     *
     * @param user имя пользователя
     */
    fun user(user: String) {
        context.user(user)
    }

    /**
     * Устанавливает пароль
     *
     * @param password пароль
     */
    fun password(password: String) {
        context.password(password)
    }

    /**
     * Устанавливает путь к файлу вывода
     *
     * @param path путь к файлу
     */
    fun output(path: Path) {
        context.output(path)
    }

    /**
     * Устанавливает код языка интерфейса
     *
     * @param code код языка
     */
    fun language(code: String) {
        context.language(code)
    }

    /**
     * Устанавливает код локализации
     *
     * @param code код локализации
     */
    fun localization(code: String) {
        context.localization(code)
    }

    /**
     * Отключает стартовые диалоги
     */
    fun disableStartupDialogs() {
        context.disableStartupDialogs()
    }

    /**
     * Отключает стартовые сообщения
     */
    fun disableStartupMessages() {
        context.disableStartupMessages()
    }

    /**
     * Устанавливает флаг не очищать файл вывода при записи
     */
    fun noTruncate() {
        context.noTruncate()
    }

    override fun buildCommandArgs(
        command: C,
        logPath: Path?,
    ): List<String> = super.buildCommandArgs(command, null) + listOf("/Out", logPath.toString())
}
