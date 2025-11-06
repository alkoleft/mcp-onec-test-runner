# Пакет `io.github.alkoleft.mcp.infrastructure.platform.locator`

## Назначение

Пакет `infrastructure.platform.locator` содержит компоненты для поиска и валидации утилит платформы 1С:Предприятие в файловой системе. Реализует кроссплатформенный поиск с кэшированием результатов.

## Основные компоненты

### UtilityLocator

**Класс:** `io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityLocator`

Кроссплатформенный локатор утилит, реализующий простой поиск в файловой системе с кэшированием. Обнаруживает утилиты платформы 1С:Предприятие на различных операционных системах.

**Методы:**
- `locateUtility(utility: UtilityType, version: String?): UtilityLocation` - находит утилиту указанного типа и версии
- `validateUtility(location: UtilityLocation): Boolean` - проверяет валидность найденной утилиты

**Процесс поиска:**
1. Проверка кэша
2. Иерархический поиск через SearchStrategy
3. Кэширование успешного результата

### UtilityCache

**Класс:** `io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityCache`

Кэш для хранения найденных утилит. Ускоряет повторные поиски утилит.

**Методы:**
- `getCachedLocation(utility: UtilityType, version: String?): UtilityLocation?` - получает кэшированную локацию
- `store(utility: UtilityType, version: String?, location: UtilityLocation)` - сохраняет локацию в кэш
- `invalidate(utility: UtilityType, version: String?)` - инвалидирует кэш для утилиты

### UtilityValidator

**Класс:** `io.github.alkoleft.mcp.infrastructure.platform.locator.UtilityValidator`

Валидатор утилит. Проверяет, что найденная утилита действительно существует и доступна для выполнения.

**Методы:**
- `validateUtility(location: UtilityLocation): Boolean` - проверяет валидность утилиты

## Связи с другими модулями

### Зависимости

- **infrastructure.platform.search** - использует для поиска утилит
- **application.core** - использует базовые типы данных

### Используется в

- **infrastructure.platform.dsl.common.PlatformUtilities** - использует для поиска утилит

