package io.github.alkoleft.mcp.core.modules

import java.nio.file.Path


// Platform utilities
data class UtilityLocation(
    val executablePath: Path,
    val version: String?,
    val platformType: PlatformType,
)

enum class PlatformType {
    WINDOWS,
    LINUX,
    MACOS,
}

enum class UtilityType(val fileName: String) {
    DESIGNER("1cv8"),
    IBCMD("ibcmd"),
    IBSRV("ibsrv"),
    THIN_CLIENT("1cv8c"),
    THICK_CLIENT("1cv8")
}

enum class ClientMode {
    THIN, THICK, ORDINARY
}