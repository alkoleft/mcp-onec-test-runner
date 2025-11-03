package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.DesignerDsl
import io.github.alkoleft.mcp.infrastructure.utility.ifNoBlank
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Реализация BuildAction для сборки через конфигуратор 1С
 */
@Component
@ConditionalOnProperty(name = ["app.tools.builder"], havingValue = "DESIGNER")
class DesignerBuildAction(
    dsl: PlatformDsl,
) : AbstractBuildAction(dsl) {
    private lateinit var actionDsl: DesignerDsl

    override fun initDsl(properties: ApplicationProperties) {
        actionDsl =
            dsl.configurator {
                // Подключаемся к информационной базе
                connect(properties.connection.connectionString)
                properties.connection.user?.ifNoBlank { user(it) }
                properties.connection.password?.ifNoBlank { password(it) }

                // Отключаем диалоги и сообщения для автоматической работы
                disableStartupDialogs()
                disableStartupMessages()
            }
    }

    override fun loadConfiguration(
        name: String,
        path: Path,
    ) = actionDsl.loadConfigFromFiles {
        fromPath(path)
    }

    override fun loadExtension(
        name: String,
        path: Path,
    ) = actionDsl.loadConfigFromFiles {
        fromPath(path)
        extension(name)
    }

    override fun updateDb() = actionDsl.updateDBCfg {}
}
