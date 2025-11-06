# Документация кода METR

Добро пожаловать в документацию кода проекта METR (MCP 1C:Enterprise Test Runner). Эта документация содержит семантическое описание всех компонентов системы для помощи в дальнейшей разработке проекта.

## Структура документации

Документация организована по пакетам и модулям проекта. Каждый пакет имеет свой файл описания с полной информацией о компонентах, их ответственности и использовании.

### Server Layer

- **[server.md](server.md)** - MCP сервер и конфигурация

### Application Layer

- **[application-actions.md](application-actions.md)** - система действий (Actions)
- **[application-services.md](application-services.md)** - сервисы прикладного уровня
- **[application-core.md](application-core.md)** - базовые доменные модели

### Infrastructure Layer

- **[infrastructure-platform-dsl.md](infrastructure-platform-dsl.md)** - DSL для работы с платформой 1С
- **[infrastructure-platform-locator.md](infrastructure-platform-locator.md)** - поиск утилит платформы
- **[infrastructure-platform-search.md](infrastructure-platform-search.md)** - стратегии поиска
- **[infrastructure-storage.md](infrastructure-storage.md)** - хранение состояния сборки
- **[infrastructure-yaxunit.md](infrastructure-yaxunit.md)** - интеграция с YaXUnit
- **[infrastructure-utility.md](infrastructure-utility.md)** - утилиты

### Configuration Layer

- **[configuration.md](configuration.md)** - конфигурация приложения

### Общие документы

- **[ARCHITECTURE.md](ARCHITECTURE.md)** - общая архитектура системы
- **[PROGRESS.md](PROGRESS.md)** - прогресс документирования

## Быстрый старт

1. Начните с [ARCHITECTURE.md](ARCHITECTURE.md) для понимания общей архитектуры системы
2. Изучите [server.md](server.md) для понимания точки входа через MCP
3. Ознакомьтесь с [application-actions.md](application-actions.md) для понимания системы действий
4. Изучите [infrastructure-platform-dsl.md](infrastructure-platform-dsl.md) для понимания работы с платформой 1С

## Навигация по компонентам

### По функциональности

**Запуск тестов:**
- [server.md](server.md) - MCP инструменты для запуска тестов
- [application-services.md](application-services.md) - LauncherService
- [application-actions.md](application-actions.md) - RunTestAction, YaXUnitTestAction
- [infrastructure-yaxunit.md](infrastructure-yaxunit.md) - YaXUnitRunner

**Сборка проекта:**
- [application-services.md](application-services.md) - LauncherService.build()
- [application-actions.md](application-actions.md) - BuildAction
- [infrastructure-platform-dsl.md](infrastructure-platform-dsl.md) - DesignerDsl, IbcmdDsl

**Анализ изменений:**
- [application-actions.md](application-actions.md) - ChangeAnalysisAction
- [infrastructure-storage.md](infrastructure-storage.md) - FileBuildStateManager

**Работа с платформой 1С:**
- [infrastructure-platform-dsl.md](infrastructure-platform-dsl.md) - все DSL
- [infrastructure-platform-locator.md](infrastructure-platform-locator.md) - поиск утилит

## Формат документации

Каждый файл документации содержит:

1. **Назначение** - описание назначения пакета
2. **Основные компоненты** - список и описание всех компонентов
3. **Методы и свойства** - описание публичных API
4. **Примеры использования** - примеры кода
5. **Связи с другими модулями** - зависимости и использование
6. **Диаграммы** - визуализация взаимодействий (где применимо)

## KDoc комментарии

Все публичные классы, функции и важные свойства в коде имеют KDoc комментарии с описанием:
- Назначения компонента
- Параметров функций
- Возвращаемых значений
- Примеров использования (где применимо)
- Примечаний и ограничений

## Обновление документации

При добавлении новых компонентов или изменении существующих:

1. Обновите соответствующий файл описания пакета
2. Добавьте KDoc комментарии в код
3. Обновите [PROGRESS.md](PROGRESS.md) при необходимости
4. Обновите [ARCHITECTURE.md](ARCHITECTURE.md) при архитектурных изменениях

## Контакты и поддержка

Для вопросов и предложений по документации создавайте issues в репозитории проекта.

