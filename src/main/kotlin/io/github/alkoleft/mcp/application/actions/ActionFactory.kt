package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.application.actions.build.DesignerBuildAction
import io.github.alkoleft.mcp.application.actions.build.IbcmdBuildAction
import io.github.alkoleft.mcp.application.actions.change.FileSystemChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.change.SourceSetChangeAnalyzer
import io.github.alkoleft.mcp.application.actions.convert.InteractiveSessionConvertAction
import io.github.alkoleft.mcp.application.actions.test.YaXUnitTestAction
import io.github.alkoleft.mcp.configuration.properties.BuilderType
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator
import io.github.alkoleft.mcp.infrastructure.storage.FileBuildStateManager
import io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParser
import org.springframework.stereotype.Component

/**
 * Фабрика для создания Actions
 */
@Component
class ActionFactory(
    private val platformDsl: PlatformDsl,
    private val utilLocator: UtilityLocator,
    private val reportParser: ReportParser,
    private val buildStateManager: FileBuildStateManager,
) {
    private val sourceSetAnalyzer: SourceSetChangeAnalyzer = SourceSetChangeAnalyzer()

    fun createBuildAction(type: BuilderType): BuildAction =
        when (type) {
            BuilderType.DESIGNER -> DesignerBuildAction(platformDsl)
            BuilderType.IBMCMD -> IbcmdBuildAction(platformDsl)
        }

    fun convertAction(): ConvertAction = InteractiveSessionConvertAction(platformDsl)

    fun createChangeAnalysisAction(): ChangeAnalysisAction = FileSystemChangeAnalysisAction(buildStateManager, sourceSetAnalyzer)

    fun createRunTestAction(): RunTestAction = YaXUnitTestAction(platformDsl, utilLocator, reportParser)
}
