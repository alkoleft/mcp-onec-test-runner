package io.github.alkoleft.mcp.infrastructure.platform.detection

import io.github.alkoleft.mcp.core.modules.PlatformType

/**
 * Platform detection utility for determining current operating system
 */
open class PlatformDetector {
    val current: PlatformType by lazy {
        val osName = System.getProperty("os.name").lowercase()
        when {
            osName.contains("win") -> PlatformType.WINDOWS
            osName.contains("mac") -> PlatformType.MACOS
            else -> PlatformType.LINUX
        }
    }

    fun isWindows(): Boolean = current == PlatformType.WINDOWS
    fun isLinux(): Boolean = current == PlatformType.LINUX
    fun isMacOS(): Boolean = current == PlatformType.MACOS
} 