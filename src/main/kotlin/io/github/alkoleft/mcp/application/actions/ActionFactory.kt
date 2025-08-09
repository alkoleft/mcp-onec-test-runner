package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.application.actions.build.DesignerBuildAction
import io.github.alkoleft.mcp.application.actions.build.IbcmdBuildAction
import io.github.alkoleft.mcp.application.actions.change.FileSystemChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.change.SourceSetChangeAnalyzer
import io.github.alkoleft.mcp.application.actions.test.YaXUnitTestAction
import io.github.alkoleft.mcp.configuration.properties.BuilderType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator
import io.github.alkoleft.mcp.infrastructure.storage.FileBuildStateManager
import io.github.alkoleft.mcp.infrastructure.yaxunit.EnhancedReportParser

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
    private val platformDsl: PlatformDsl,
    private val utilLocator: UtilityLocator,
    private val reportParser: EnhancedReportParser,
    private val buildStateManager: FileBuildStateManager,
) : ActionFactory {
    private val sourceSetAnalyzer: SourceSetChangeAnalyzer = SourceSetChangeAnalyzer()

    override fun createBuildAction(type: BuilderType): BuildAction =
        when (type) {
            BuilderType.DESIGNER -> DesignerBuildAction(platformDsl)
            BuilderType.IBMCMD -> IbcmdBuildAction(platformDsl)
        }

    override fun createChangeAnalysisAction(): ChangeAnalysisAction = FileSystemChangeAnalysisAction(buildStateManager, sourceSetAnalyzer)

    override fun createRunTestAction(): RunTestAction = YaXUnitTestAction(platformDsl, utilLocator, reportParser)
}
