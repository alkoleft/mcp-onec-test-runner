package io.github.alkoleft.mcp.configuration.properties

import java.nio.file.Path
import kotlin.io.path.Path

class SourceSet(
    val basePath: Path = Path(""),
    items: Collection<SourceSetItem>? = emptyList(),
) : ArrayList<SourceSetItem>(items) {
    val configuration
        get() = find { it.type == SourceSetType.CONFIGURATION }
    val extensions
        get() = filter { it.type == SourceSetType.EXTENSION }

    fun subSourceSet(predicate: (SourceSetItem) -> Boolean) = SourceSet(basePath, filter(predicate))

    fun byName(name: String) = find { it.name == name } ?: throw ClassNotFoundException("Не обнаружен набор исходников с именем $name")

    fun pathByName(name: String): Path = basePath.resolve(byName(name).path)

    companion object {
        val EMPTY: SourceSet = SourceSet(Path(""))
    }
}
