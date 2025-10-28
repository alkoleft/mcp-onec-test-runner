package io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.V8Context

/**
 * Контекст для работы с 1С:Предприятие
 */
class EnterpriseContext(
    platformContext: PlatformUtilityContext,
) : V8Context(platformContext) {
    var runArguments: String? = null

    /**
     * Строит аргументы для запуска 1С:Предприятие
     */
    override fun buildBaseArgs(): List<String> =
        buildCommonArgs(UtilityType.THIN_CLIENT, "ENTERPRISE")
            .also { args ->
                runArguments?.let {
                    args.add("/C")
                    args.add(it)
                }
            }
}
