package io.github.alkoleft.mcp.infrastructure.platform.dsl.common

fun String.ifNoBlank(block: (String) -> Unit): String {
    if (isNotBlank()) {
        block(this)
    }
    return this
}