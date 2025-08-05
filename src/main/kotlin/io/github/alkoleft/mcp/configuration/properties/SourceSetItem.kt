package io.github.alkoleft.mcp.configuration.properties

/**
 * Элемент набора исходного кода
 */
data class SourceSetItem(
    val path: String,
    val name: String,
    val type: SourceSetType,
    val purpose: Set<SourceSetPurpose> = emptySet()
)