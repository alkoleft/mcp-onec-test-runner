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
            "Путь элемента source set не может быть пустым"
        }
        require(name.isNotBlank()) {
            "Имя элемента source set не может быть пустым"
        }

        // Проверяем, что путь не содержит недопустимые символы
        require(!path.contains("..")) {
            "Путь source set не может содержать '..': $path"
        }

        // Проверяем, что путь не начинается с слеша (должен быть относительным)
        require(!path.startsWith("/")) {
            "Путь source set должен быть относительным: $path"
        }

        // Проверяем, что имя не содержит недопустимые символы
        require(!name.contains("/") && !name.contains("\\")) {
            "Имя source set не может содержать разделители пути: $name"
        }

        // Проверяем, что тип имеет допустимое значение
        require(type in SourceSetType.entries.toTypedArray()) {
            "Недопустимый тип source set: $type"
        }

        // Проверяем, что все назначения имеют допустимые значения
        purpose.forEach { purposeItem ->
            require(purposeItem in SourceSetPurpose.entries.toTypedArray()) {
                "Недопустимое назначение source set: $purposeItem"
            }
        }
    }
}
