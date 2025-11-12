# Пакет `io.github.alkoleft.mcp.configuration`

## Назначение

Пакет `configuration` содержит классы конфигурации приложения, загружаемые из файлов `application.yml` или `application.properties`. Обеспечивает валидацию конфигурации и типизированный доступ к настройкам.

## Основные компоненты

### ApplicationProperties

**Класс:** `io.github.alkoleft.mcp.configuration.properties.ApplicationProperties`

Основные свойства приложения с валидацией конфигурации. Загружается из конфигурационных файлов с префиксом `app`.

**Свойства:**
- `id: String?` - идентификатор проекта (опционально)
- `format: ProjectFormat` - формат проекта (DESIGNER или EDT)
- `basePath: Path` - базовый путь к проекту
- `sourceSet: SourceSet` - описание исходных файлов проекта
- `connection: ConnectionProperties` - параметры подключения к информационной базе
- `platformVersion: String` - версия платформы 1С
- `tools: ToolsProperties` - настройки инструментов

**Вычисляемые свойства:**
- `workPath: Path` - рабочая директория для временных файлов (ленивое вычисление)

**Валидация:**
- Валидация базового пути (существование, доступность)
- Валидация source set (наличие обязательных элементов, уникальность путей)
- Валидация подключения (наличие строки подключения)
- Валидация версии платформы
- Валидация инструментов

### SourceSet

**Класс:** `io.github.alkoleft.mcp.configuration.properties.SourceSet`

Набор исходных файлов проекта. Содержит список элементов source set.

**Свойства:**
- `basePath: Path` - базовый путь к source set
- `items: List<SourceSetItem>` - список элементов source set

**Методы:**
- `configuration: SourceSetItem?` - основная конфигурация
- `extensions: List<SourceSetItem>` - список расширений
- `subSourceSet(predicate: (SourceSetItem) -> Boolean): SourceSet` - фильтрация source set

### SourceSetItem

**Класс:** `io.github.alkoleft.mcp.configuration.properties.SourceSetItem`

Элемент source set, описывающий один модуль проекта.

**Свойства:**
- `path: String` - относительный путь к модулю
- `name: String` - имя модуля
- `type: SourceSetType` - тип модуля (CONFIGURATION или EXTENSION)
- `purpose: List<SourceSetPurpose>` - назначение модуля (MAIN, TESTS, YAXUNIT)

### ConnectionProperties

**Класс:** `io.github.alkoleft.mcp.configuration.properties.ConnectionProperties`

Параметры подключения к информационной базе 1С:Предприятие.

**Свойства:**
- `connectionString: String` - строка подключения к информационной базе
- `user: String?` - имя пользователя (опционально)
- `password: String?` - пароль (опционально)

### ToolsProperties

**Класс:** `io.github.alkoleft.mcp.configuration.properties.ToolsProperties`

Настройки инструментов для работы с проектом.

**Свойства:**
- `builder: BuilderType` - тип сборщика (DESIGNER или IBCMD)
- `edtCli: EdtCliProperties?` - настройки EDT CLI (опционально)

### EdtCliProperties

**Класс:** `io.github.alkoleft.mcp.configuration.properties.EdtCliProperties`

Настройки 1C:EDT CLI.

**Свойства:**
- `autoStart: Boolean` - автозапуск EDT CLI при старте приложения
- `version: String` - версия EDT CLI
- `workingDirectory: String?` - рабочая директория EDT
- `startupTimeoutMs: Long` - таймаут запуска EDT CLI
- `commandTimeoutMs: Long` - таймаут выполнения команды
- `readyCheckTimeoutMs: Long` - таймаут проверки готовности

### Enums

**Файл:** `io.github.alkoleft.mcp.configuration.properties.Enums`

Перечисления для конфигурации:
- `ProjectFormat` - формат проекта (DESIGNER, EDT)
- `BuilderType` - тип сборщика (DESIGNER, IBCMD)
- `SourceSetType` - тип source set (CONFIGURATION, EXTENSION)
- `SourceSetPurpose` - назначение source set (MAIN, TESTS, YAXUNIT)

### JacksonConfig

**Класс:** `io.github.alkoleft.mcp.configuration.JacksonConfig`

Конфигурация Jackson для сериализации/десериализации JSON. Настраивает ObjectMapper для работы с Kotlin классами.

## Связи с другими модулями

### Зависимости

- **Spring Boot Configuration Properties** - используется для загрузки конфигурации
- **Jackson** - используется для сериализации/десериализации

### Используется в

- Используется во всех слоях приложения для получения конфигурации

## Пример конфигурации

```yaml
app:
  id: my-project
  format: EDT
  base-path: /path/to/project
  source-set:
    - path: "configuration"
      name: MyConfig
      type: CONFIGURATION
      purpose: [MAIN]
    - path: "tests"
      name: Tests
      type: EXTENSION
      purpose: [TESTS, YAXUNIT]
  connection:
    connection-string: "File='/path/to/db';"
    user: admin
    password: password
  platform-version: "8.3.24.1234"
  tools:
    builder: DESIGNER
    edt-cli:
      auto-start: true
      version: "latest"
      working-directory: "/path/to/edt-workspace"
```

