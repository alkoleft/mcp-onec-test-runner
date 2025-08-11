package io.github.alkoleft.mcp.configuration.properties

/**
 * Элемент набора исходного кода
 */
data class SourceSetItem(
    val path: String,
    val name: String,
    val type: SourceSetType,
    val purpose: Set<SourceSetPurpose> = emptySet(),
) {
    init {
        validateSourceSetItem()
    }

    /**
     * Валидация элемента набора исходного кода
     */
    private fun validateSourceSetItem() {
        require(path.isNotBlank()) {
            "Source set item path cannot be empty"
        }
        require(name.isNotBlank()) {
            "Source set item name cannot be empty"
        }

        // Проверяем, что путь не содержит недопустимые символы
        require(!path.contains("..")) {
            "Source set path cannot contain '..': $path"
        }

        // Проверяем, что путь не начинается с слеша (должен быть относительным)
        require(!path.startsWith("/")) {
            "Source set path must be relative: $path"
        }

        // Проверяем, что имя не содержит недопустимые символы
        require(!name.contains("/") && !name.contains("\\")) {
            "Source set name cannot contain path separators: $name"
        }

        // Проверяем, что тип имеет допустимое значение
        require(type in SourceSetType.entries.toTypedArray()) {
            "Invalid source set type: $type"
        }

        // Проверяем, что все назначения имеют допустимые значения
        purpose.forEach { purposeItem ->
            require(purposeItem in SourceSetPurpose.entries.toTypedArray()) {
                "Invalid source set purpose: $purposeItem"
            }
        }
    }
}
