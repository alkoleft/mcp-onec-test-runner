package io.github.alkoleft.mcp.infrastructure.utility

fun String.ifNoBlank(block: (String) -> Unit): String {
    if (isNotBlank()) {
        block(this)
    }
    return this
}