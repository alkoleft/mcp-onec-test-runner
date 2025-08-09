package io.github.alkoleft.mcp.infrastructure.platform.dsl.enterprise

import io.github.alkoleft.mcp.core.modules.UtilityType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.BasePlatformContext
import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext

/**
 * Контекст для работы с 1С:Предприятие
 */
class EnterpriseContext(
    platformContext: PlatformUtilityContext,
) : BasePlatformContext(platformContext) {
    var runArguments: String? = null

    /**
     * Строит аргументы для запуска 1С:Предприятие
     */
    suspend fun buildBaseArgs(): List<String> =
        buildCommonArgs(UtilityType.THIN_CLIENT, "ENTERPRISE")
            .also { args ->
                runArguments?.let {
                    args.add("/C")
                    args.add(it)
                }
            }
}
