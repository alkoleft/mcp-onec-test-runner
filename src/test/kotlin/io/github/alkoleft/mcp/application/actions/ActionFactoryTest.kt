package io.github.alkoleft.mcp.application.actions

import io.github.alkoleft.mcp.application.actions.build.DesignerBuildAction
import io.github.alkoleft.mcp.application.actions.build.IbcmdBuildAction
import io.github.alkoleft.mcp.application.actions.change.FileSystemChangeAnalysisAction
import io.github.alkoleft.mcp.application.actions.test.YaXUnitTestAction
import io.github.alkoleft.mcp.configuration.properties.BuilderType
import io.github.alkoleft.mcp.core.modules.FileWatcher
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.alkoleft.mcp.infrastructure.platform.CrossPlatformUtilLocator
import io.github.alkoleft.mcp.infrastructure.process.JsonYaXUnitConfigWriter
import io.github.alkoleft.mcp.infrastructure.process.EnhancedReportParser
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ActionFactoryTest {

    @Test
    fun `should create correct build action for each type`() {
        val platformUtilityDsl = mockk<PlatformUtilityDsl>()
        val utilLocator = mockk<CrossPlatformUtilLocator>()
        val configWriter = mockk<JsonYaXUnitConfigWriter>()
        val reportParser = mockk<EnhancedReportParser>()
        val factory = ActionFactoryImpl(platformUtilityDsl, utilLocator, configWriter, reportParser)

        val designerAction = factory.createBuildAction(BuilderType.DESIGNER)
        assertTrue(designerAction is DesignerBuildAction)

        val ibcmdAction = factory.createBuildAction(BuilderType.IBMCMD)
        assertTrue(ibcmdAction is IbcmdBuildAction)
    }

    @Test
    fun `should create correct change analysis action`() {
        val platformUtilityDsl = mockk<PlatformUtilityDsl>()
        val utilLocator = mockk<CrossPlatformUtilLocator>()
        val configWriter = mockk<JsonYaXUnitConfigWriter>()
        val reportParser = mockk<EnhancedReportParser>()
        val factory = ActionFactoryImpl(platformUtilityDsl, utilLocator, configWriter, reportParser)

        val action = factory.createChangeAnalysisAction()
        assertTrue(action is FileSystemChangeAnalysisAction)
    }

    @Test
    fun `should create correct run test action`() {
        val platformUtilityDsl = mockk<PlatformUtilityDsl>()
        val utilLocator = mockk<CrossPlatformUtilLocator>()
        val configWriter = mockk<JsonYaXUnitConfigWriter>()
        val reportParser = mockk<EnhancedReportParser>()
        val factory = ActionFactoryImpl(platformUtilityDsl, utilLocator, configWriter, reportParser)

        val action = factory.createRunTestAction()
        assertTrue(action is YaXUnitTestAction)
    }
} 