package io.github.alkoleft.mcp.configuration.properties


/**
 * Тип элемента исходного кода
 */
enum class SourceSetType {
    CONFIGURATION,
    EXTENSION
}

/**
 * Тип сборщика
 */
enum class BuilderType {
    DESIGNER,
    IBMCMD
}

/**
 * Назначение элемента исходного кода
 */
enum class SourceSetPurpose {
    MAIN,      // Основной код продукта
    TESTS,     // Тесты
    YAXUNIT    // Движок YaXUnit
}