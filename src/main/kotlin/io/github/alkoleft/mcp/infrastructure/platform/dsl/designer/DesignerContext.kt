package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.V8Context

/**
 * Контекст для работы с конфигуратором 1С
 *
 * Содержит базовые настройки и методы для построения аргументов команд
 */
class DesignerContext(
    platformContext: PlatformUtilityContext,
) : V8Context(platformContext) {
    /**
     * Строит базовые аргументы для команд конфигуратора
     */
    override fun buildBaseArgs(): List<String> = buildCommonArgs(UtilityType.DESIGNER, "DESIGNER")
}
