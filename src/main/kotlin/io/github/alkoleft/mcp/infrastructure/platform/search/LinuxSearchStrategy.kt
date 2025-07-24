package io.github.alkoleft.mcp.infrastructure.platform.search

import java.nio.file.Paths

/**
 * Linux-specific search strategy
 */
class LinuxSearchStrategy : SearchStrategy {
    override val tier1Locations = listOf(
        StandardLocation("/opt/1cv8"),
        StandardLocation("/usr/local/1cv8"),
    )

    override val tier2Locations = listOf(
        VersionLocation("/opt/1cv8"),
        VersionLocation("/usr/local/1cv8"),
    )

    override val tier3Locations = listOf(
        PathEnvironmentLocation(),
    )
} 