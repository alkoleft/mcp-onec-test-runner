package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.application.actions.build.DesignerBuildAction
import io.github.alkoleft.mcp.application.actions.build.IbcmdBuildAction
import io.github.alkoleft.mcp.application.actions.change.FileSystemChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.test.YaXUnitTestAction
import io.github.alkoleft.mcp.configuration.properties.BuilderType
import io.github.alkoleft.mcp.core.modules.FileWatcher
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class SimpleActionTest {

    @Test
    fun `should create action factory with all action types`() {
        val platformUtilityDsl = mockk<PlatformUtilityDsl>()
        val fileWatcher = mockk<FileWatcher>()
        val factory = ActionFactoryImpl(platformUtilityDsl)

        // Test that factory can create all action types
        val designerAction = factory.createBuildAction(BuilderType.DESIGNER)
        assertTrue(designerAction is DesignerBuildAction)

        val ibcmdAction = factory.createBuildAction(BuilderType.IBMCMD)
        assertTrue(ibcmdAction is IbcmdBuildAction)

        val changeAction = factory.createChangeAnalysisAction()
        assertTrue(changeAction is FileSystemChangeAnalysisAction)

        val testAction = factory.createRunTestAction()
        assertTrue(testAction is YaXUnitTestAction)
    }

    @Test
    fun `should create action implementations`() {
        val platformUtilityDsl = mockk<PlatformUtilityDsl>()
        val fileWatcher = mockk<FileWatcher>()

        // Test that all action implementations can be instantiated
        val designerAction = DesignerBuildAction(platformUtilityDsl)
        assertTrue(designerAction is BuildAction)

        val ibcmdAction = IbcmdBuildAction(platformUtilityDsl)
        assertTrue(ibcmdAction is BuildAction)

        val changeAction = FileSystemChangeAnalysisAction(fileWatcher)
        assertTrue(changeAction is ChangeAnalysisAction)

        val testAction = YaXUnitTestAction(platformUtilityDsl)
        assertTrue(testAction is RunTestAction)
    }
} 