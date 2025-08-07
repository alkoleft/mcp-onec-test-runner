package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.application.actions.build.DesignerBuildAction
import io.github.alkoleft.mcp.application.actions.build.IbcmdBuildAction
import io.github.alkoleft.mcp.application.actions.change.FileSystemChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.test.YaXUnitTestAction
import io.github.alkoleft.mcp.configuration.properties.BuilderType
import io.github.alkoleft.mcp.core.modules.FileWatcher
import io.github.alkoleft.mcp.core.modules.FileWatcherImpl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.process.JsonYaXUnitConfigWriter
import io.github.alkoleft.mcp.infrastructure.process.EnhancedReportParser

/**
 * Фабрика для создания Actions
 */
interface ActionFactory {
    fun createBuildAction(type: BuilderType): BuildAction
    fun createChangeAnalysisAction(): ChangeAnalysisAction
    fun createRunTestAction(): RunTestAction
}

/**
 * Реализация ActionFactory
 */
class ActionFactoryImpl(
    private val platformUtilityDsl: PlatformUtilityDsl,
    private val utilLocator: CrossPlatformUtilLocator,
    private val configWriter: JsonYaXUnitConfigWriter,
    private val reportParser: EnhancedReportParser
) : ActionFactory {
    private val fileWatcher: FileWatcher = FileWatcherImpl()
    override fun createBuildAction(type: BuilderType): BuildAction {
        return when (type) {
            BuilderType.DESIGNER -> DesignerBuildAction(platformUtilityDsl)
            BuilderType.IBMCMD -> IbcmdBuildAction(platformUtilityDsl)
        }
    }

    override fun createChangeAnalysisAction(): ChangeAnalysisAction {
        return FileSystemChangeAnalysisAction(fileWatcher)
    }

    override fun createRunTestAction(): RunTestAction {
        return YaXUnitTestAction(platformUtilityDsl, utilLocator, configWriter, reportParser)
    }
} 