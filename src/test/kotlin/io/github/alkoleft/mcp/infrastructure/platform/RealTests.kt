package io.github.alkoleft.mcp.infrastructure.platform

import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import kotlin.io.path.Path

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class RealTests(
    @Autowired private val platformDsl: PlatformUtilityDsl
) {
    @Test
    fun designerRealExecute() {
        val plan = platformDsl.configuratorPlan("8.3.24.1761") {
            connectToFile("/home/alko/develop/onec_file_db/YaxUnit-dev")
            disableStartupDialogs()
            disableStartupMessages()
            loadConfigFromFiles {
                fromPath(Path("/home/alko/Downloads/sources/configuration"))
            }
            listOf("yaxunit", "smoke", "tests").forEach {
                loadConfigFromFiles {
                    fromPath(Path("/home/alko/Downloads/sources/$it"))
                    extension = it
                }
            }
        }.buildPlan()
        plan.printPlan()
        runBlocking { plan.execute() }
    }

    @Test
    fun ibcmdRealExecute() {
        val plan = platformDsl.ibcmd("8.3.24.1761") {
            dbPath("/home/alko/develop/onec_file_db/YaxUnit-dev")

            config {
                import("/home/alko/Downloads/sources/configuration")
                listOf("yaxunit", "smoke", "tests").forEach {
                    import("/home/alko/Downloads/sources/$it"){
                        extension = it
                    }
                }

            }
        }
        plan.printPlan()
        runBlocking { plan.execute() }
    }
}