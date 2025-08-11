package io.github.alkoleft.mcp

import io.github.alkoleft.mcp.application.services.LauncherService
import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Ignore

@Ignore
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@ActiveProfiles("yaxunit-edt")
class LaunchTest(
    @Autowired private val launcher: LauncherService,
    @Autowired private val properties: ApplicationProperties,
) {
    @Test
    fun launchYaxUnit() {
        runBlocking {
            launcher.run(RunModuleTestsRequest("ОМ_ЮТКоллекции", properties))
        }
    }
}
