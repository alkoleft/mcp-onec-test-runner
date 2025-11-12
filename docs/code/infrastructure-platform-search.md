# Пакет `io.github.alkoleft.mcp.infrastructure.platform.search`

## Назначение

Пакет `infrastructure.platform.search` содержит стратегии поиска утилит платформы 1С:Предприятие в файловой системе. Реализует различные алгоритмы поиска для разных типов утилит и операционных систем.

## Основные компоненты

### SearchStrategy

**Интерфейс:** `io.github.alkoleft.mcp.infrastructure.platform.search.SearchStrategy`

Интерфейс для стратегий поиска утилит. Определяет метод поиска утилиты указанного типа и версии.

**Методы:**
- `search(utility: UtilityType, version: String?): UtilityLocation` - выполняет поиск утилиты

### SearchStrategyFactory

**Класс:** `io.github.alkoleft.mcp.infrastructure.platform.search.SearchStrategyFactory`

Фабрика для создания стратегий поиска. Создает подходящую стратегию поиска для указанного типа утилиты.

**Методы:**
- `createSearchStrategy(utility: UtilityType): SearchStrategy` - создает стратегию поиска

### SearchLocation

**Класс:** `io.github.alkoleft.mcp.infrastructure.platform.search.SearchLocation`

Представляет локацию для поиска утилит (путь и описание).

### VersionResolver

**Класс:** `io.github.alkoleft.mcp.infrastructure.platform.search.VersionResolver`

Резолвер версий утилит. Определяет версию утилиты по пути к исполняемому файлу.

### Version

**Класс:** `io.github.alkoleft.mcp.infrastructure.platform.search.Version`

Представляет версию утилиты с методами сравнения.

## Связи с другими модулями

### Зависимости

- **application.core** - использует базовые типы данных

### Используется в

- **infrastructure.platform.locator.UtilityLocator** - использует для поиска утилит

