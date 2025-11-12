# Пакет `io.github.alkoleft.mcp.infrastructure.yaxunit`

## Назначение

Пакет `infrastructure.yaxunit` содержит компоненты для интеграции с фреймворком тестирования YaXUnit. Обеспечивает запуск тестов, парсинг отчетов и обработку логов.

## Основные компоненты

### YaXUnitRunner

**Класс:** `io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitRunner`

Реализация запуска тестов YaXUnit через 1С:Предприятие. Интегрирован со стратегиями построения команд и обработки ошибок.

**Методы:**
- `executeTests(request: TestExecutionRequest): YaXUnitExecutionResult` - выполняет запуск тестов

**Процесс:**
1. Создание временной конфигурации YaXUnit
2. Запуск тестов через EnterpriseDsl
3. Определение путей к отчетам и логам

### ReportParser

**Класс:** `io.github.alkoleft.mcp.infrastructure.yaxunit.ReportParser`

Парсер отчетов о тестировании YaXUnit. Парсит XML отчеты в формате JUnit и преобразует их в универсальную структуру GenericTestReport.

**Методы:**
- `parseReport(inputStream: InputStream): GenericTestReport` - парсит отчет из потока

### LogParser

**Класс:** `io.github.alkoleft.mcp.infrastructure.yaxunit.LogParser`

Парсер логов выполнения тестов YaXUnit. Извлекает ошибки и предупреждения из логов.

**Методы:**
- `extractErrors(logPath: String): List<String>` - извлекает ошибки из лога

### YaXUnitConfig

**Класс:** `io.github.alkoleft.mcp.infrastructure.yaxunit.YaXUnitConfig`

Конфигурация для запуска тестов YaXUnit. Содержит параметры запуска тестов, пути к отчетам и настройки логирования.

**Свойства:**
- `reportPath: String` - путь к файлу отчета
- `logging: LoggingConfig` - настройки логирования
- И другие параметры конфигурации

**Методы:**
- `validate(): ValidationResult` - валидирует конфигурацию

### Paths

**Объект:** `io.github.alkoleft.mcp.infrastructure.yaxunit.Paths`

Утилиты для работы с путями YaXUnit. Содержит константы и функции для генерации путей к конфигурационным файлам и отчетам.

## Связи с другими модулями

### Зависимости

- **infrastructure.platform.dsl** - использует для запуска тестов
- **application.actions.test.yaxunit** - использует модели данных тестов
- **Jackson** - используется для сериализации/десериализации конфигурации

### Используется в

- **application.actions.test.yaxunit.YaXUnitTestAction** - использует для запуска тестов и парсинга отчетов

