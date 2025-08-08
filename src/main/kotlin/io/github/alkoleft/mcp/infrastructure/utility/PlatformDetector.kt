package io.github.alkoleft.mcp.infrastructure.utility

import io.github.alkoleft.mcp.core.modules.PlatformType

/**
 * Platform detection utility for determining current operating system
 */
object PlatformDetector {

    val current: PlatformType by lazy {
        val osName = System.getProperty("os.name").lowercase()
        when {
            osName.contains("win") -> PlatformType.WINDOWS
            osName.contains("mac") -> PlatformType.MACOS
            else -> PlatformType.LINUX
        }
    }

    val isWindows: Boolean by lazy { current == PlatformType.WINDOWS }
    val isLinux: Boolean by lazy { current == PlatformType.LINUX }
    val isMacOS: Boolean by lazy { current == PlatformType.MACOS }
}