package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.BasePlatformContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext

/**
 * Контекст для работы с конфигуратором 1С
 *
 * Содержит базовые настройки и методы для построения аргументов команд
 */
class DesignerContext(
    platformContext: PlatformUtilityContext
) : BasePlatformContext(platformContext) {
    /**
     * Строит базовые аргументы для команд конфигуратора
     */
    suspend fun buildBaseArgs(): List<String> = buildCommonArgs(UtilityType.DESIGNER, "DESIGNER")
} 