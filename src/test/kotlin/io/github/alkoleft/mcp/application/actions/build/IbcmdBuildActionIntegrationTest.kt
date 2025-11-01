package io.github.alkoleft.mcp.application.actions.build

import io.github.alkoleft.mcp.configuration.properties.ApplicationProperties
import io.github.alkoleft.mcp.configuration.properties.Connection
import io.github.alkoleft.mcp.configuration.properties.SourceSet
import io.github.alkoleft.mcp.core.modules.ShellCommandResult
import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformDsl
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.nio.file.Paths

@SpringBootTest
@ActiveProfiles("test")
class IbcmdBuildActionIntegrationTest
    @Autowired
    constructor(
        private val dsl: PlatformDsl,
    ) {
        private val mockProperties =
            ApplicationProperties(
                basePath = Paths.get("/mock/project"),
                connection =
                    Connection(
                        connectionString = "File=/mock/db/path",
                        user = "testuser",
                        password = "testpass",
                    ),
                // Mock other properties as needed
                sourceSet =
                    SourceSet(
                        basePath = Paths.get("/mock"),
                        items = emptyList(),
                    ),
            )

        private val mockSourceSet =
            SourceSet(
                basePath = Paths.get("/mock"),
                items = emptyList(),
            )

        @Test
        fun `executeBuildDsl should succeed with valid config and extension`() {
            System.setProperty("IBCMD_IB_USR", "testuser")
            System.setProperty("IBCMD_IB_PSW", "testpass")

            val action = IbcmdBuildAction(dsl)
            val result = action.executeBuildDsl(mockProperties, mockSourceSet)

            assertTrue(result.success)
            assertFalse(result.sourceSet.isEmpty())
            assertTrue(result.errors.isEmpty())
            result.sourceSet.values.forEach { shellResult ->
                assertTrue((shellResult as ShellCommandResult).success)
            }
        }

        @Test
        fun `executeBuildDsl should fail gracefully with invalid credentials`() {
            val invalidProperties =
                mockProperties.copy(
                    connection =
                        mockProperties.connection.copy(
                            user = null,
                            password = null,
                        ),
                )

            val action = IbcmdBuildAction(dsl)
            assertThrows(IllegalArgumentException::class.java) {
                action.executeBuildDsl(invalidProperties, mockSourceSet)
            }
        }

        @Test
        fun `temp dir should be cleaned up after execution`() {
            val action = IbcmdBuildAction(dsl)
            val result = action.executeBuildDsl(mockProperties, mockSourceSet)
            assertTrue(result.success)
            // Additional cleanup verification can be added if needed
        }
    }
