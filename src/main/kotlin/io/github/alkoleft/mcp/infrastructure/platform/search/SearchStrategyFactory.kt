package io.github.alkoleft.mcp.infrastructure.platform.search

import io.github.alkoleft.mcp.core.modules.PlatformType
import io.github.alkoleft.mcp.infrastructure.platform.detection.PlatformDetector

/**
 * Factory for creating platform-specific search strategies
 */
class SearchStrategyFactory(private val platformDetector: PlatformDetector) {
    
    /**
     * Creates platform-specific search strategy
     */
    fun createSearchStrategy(): SearchStrategy =
        when (platformDetector.current) {
            PlatformType.WINDOWS -> WindowsSearchStrategy()
            PlatformType.LINUX -> LinuxSearchStrategy()
            PlatformType.MACOS -> LinuxSearchStrategy() // MacOS uses similar paths to Linux
        }
} 