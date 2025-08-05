package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring configuration for Actions
 */
@Configuration
class ActionConfiguration {

    @Bean
    fun actionFactory(
        platformUtilityDsl: PlatformUtilityDsl
    ): ActionFactory = ActionFactoryImpl(platformUtilityDsl)
} 