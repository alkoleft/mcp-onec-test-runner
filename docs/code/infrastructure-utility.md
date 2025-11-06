# Пакет `io.github.alkoleft.mcp.infrastructure.utility`

## Назначение

Пакет `infrastructure.utility` содержит утилитные классы и функции для работы с платформой и строками.

## Основные компоненты

### PlatformDetector

**Класс:** `io.github.alkoleft.mcp.infrastructure.utility.PlatformDetector`

Детектор операционной системы. Определяет тип платформы (Windows, Linux, macOS).

**Методы:**
- `detectPlatform(): PlatformType` - определяет тип платформы

### StringExtensions

**Файл:** `io.github.alkoleft.mcp.infrastructure.utility.StringExtensions`

Расширяющие функции для работы со строками.

**Функции:**
- `String.ifNoBlank(block: (String) -> Unit)` - выполняет блок, если строка не пустая

## Связи с другими модулями

### Зависимости

- **application.core** - использует базовые типы данных

### Используется в

- Используется во всех слоях приложения для утилитных операций

