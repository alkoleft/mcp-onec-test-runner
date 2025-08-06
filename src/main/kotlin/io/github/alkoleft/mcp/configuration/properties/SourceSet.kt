package io.github.alkoleft.mcp.configuration.properties

class SourceSet : ArrayList<SourceSetItem> {
    constructor() : super()
    constructor(c: Collection<SourceSetItem>) : super(c)

    val configuration
        get() = find { it.type == SourceSetType.CONFIGURATION }
    val extensions
        get() = filter { it.type == SourceSetType.EXTENSION }

    fun subSourceSet(predicate: (SourceSetItem) -> Boolean) =
        SourceSet(filter(predicate))
}