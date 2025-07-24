# CrossPlatformUtilLocator Module Decomposition

## Overview

Модуль `CrossPlatformUtilLocator` был декомпозирован на отдельные компоненты для улучшения поддерживаемости, тестируемости и расширяемости. Алгоритм поиска упрощен и основан на поиске по известным путям файловой системы.

## Структура пакетов

```
platform/
├── cache/                    # Кэширование путей утилит
│   └── UtilPathCache.kt
├── detection/               # Определение платформы
│   └── PlatformDetector.kt
├── search/                  # Стратегии поиска
│   ├── SearchStrategy.kt
│   ├── SearchStrategyFactory.kt
│   ├── WindowsSearchStrategy.kt
│   └── LinuxSearchStrategy.kt
├── validation/              # Валидация утилит
│   └── UtilityValidator.kt
├── version/                 # Извлечение версий
│   └── VersionExtractor.kt
└── CrossPlatformUtilLocator.kt  # Основной локатор
```

## Компоненты

### 1. UtilPathCache
**Назначение**: Кэширование найденных путей утилит с TTL (24 часа)
**Основные функции**:
- `store()` - сохранение пути в кэш
- `getCachedLocation()` - получение пути из кэша
- `invalidate()` - инвалидация конкретной записи
- `clear()` - очистка всего кэша

### 2. PlatformDetector
**Назначение**: Определение текущей операционной системы
**Поддерживаемые платформы**:
- Windows
- Linux
- macOS

### 3. SearchStrategy
**Назначение**: Стратегии поиска для разных платформ
**Иерархия поиска**:
- **Tier 1**: Стандартные пути установки
- **Tier 2**: Версионные пути
- **Tier 3**: PATH переменная окружения

### 4. UtilityValidator
**Назначение**: Валидация найденных утилит
**Проверки**:
- Существование файла
- Права на выполнение
- Базовая функциональность (запуск с параметром `/?`)

### 5. VersionExtractor
**Назначение**: Извлечение и проверка версий утилит
**Функции**:
- `extractVersion()` - извлечение версии из исполняемого файла
- `isVersionCompatible()` - проверка совместимости версий

## Алгоритм поиска

### Упрощенный алгоритм
1. **Проверка кэша** - поиск в кэше с валидацией
2. **Иерархический поиск**:
   - Tier 1: Стандартные пути (`/opt/1cv8`, `C:\Program Files\1cv8`)
   - Tier 2: Версионные пути (`/opt/1cv8/8.3.24`)
   - Tier 3: PATH переменная окружения
3. **Кэширование результата** - сохранение найденного пути

### Известные пути поиска

#### Windows
- `C:\Program Files\1cv8\bin\1cv8c.exe`
- `C:\Program Files (x86)\1cv8\bin\1cv8c.exe`
- `C:\Program Files\1cv8\8.3.24\bin\1cv8c.exe`

#### Linux/macOS
- `/opt/1cv8/bin/1cv8c`
- `/usr/local/1cv8/bin/1cv8c`
- `/opt/1cv8/8.3.24/bin/1cv8c`

## Тестирование

### Структура тестов
```
src/test/kotlin/io/github/alkoleft/mcp/infrastructure/platform/
├── cache/
│   └── UtilPathCacheTest.kt
├── detection/
│   └── PlatformDetectorTest.kt
├── search/
│   ├── SearchStrategyTest.kt
│   └── SearchStrategyFactoryTest.kt
├── validation/
│   └── UtilityValidatorTest.kt
├── version/
│   └── VersionExtractorTest.kt
└── CrossPlatformUtilLocatorTest.kt
```

### Покрытие тестами
- ✅ Кэширование (UtilPathCache)
- ✅ Определение платформы (PlatformDetector)
- ✅ Стратегии поиска (SearchStrategy)
- ✅ Валидация утилит (UtilityValidator)
- ✅ Извлечение версий (VersionExtractor)
- ✅ Интеграционные тесты (CrossPlatformUtilLocator)

## Преимущества декомпозиции

1. **Модульность**: Каждый компонент имеет четкую ответственность
2. **Тестируемость**: Возможность тестировать компоненты изолированно
3. **Расширяемость**: Легко добавлять новые стратегии поиска или платформы
4. **Поддерживаемость**: Упрощенная структура кода
5. **Переиспользование**: Компоненты можно использовать в других частях системы

## Расширение функциональности

### Добавление новой платформы
1. Создать новый класс стратегии (например, `MacOSSearchStrategy`)
2. Добавить в `SearchStrategyFactory`
3. Добавить тесты

### Добавление новых путей поиска
1. Создать новый класс локации (наследовать от `BaseSearchLocation`)
2. Добавить в соответствующую стратегию
3. Обновить тесты

### Изменение алгоритма валидации
1. Модифицировать `UtilityValidator`
2. Обновить тесты валидации
3. Проверить интеграционные тесты 