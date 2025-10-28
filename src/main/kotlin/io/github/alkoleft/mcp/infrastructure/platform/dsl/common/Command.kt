package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

interface Command {
    /**
     * Полное описание команды для логирования и отладки
     */
    fun getFullDescription(): String

    /**
     * Список аргументов командной строки для данной команды
     */
    val arguments: List<String>
}