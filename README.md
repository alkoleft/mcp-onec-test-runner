# MCP YaXUnit Runner

Инструмент, который позволяет запускать тесты YaXUnit и собирать проекты 1С прямо из AI‑ассистентов (Claude, GPT, Cursor, VS Code и др.) с помощью протокола MCP (Model Context Protocol).

## Описание

MCP YAxUnit Runner — это MCP‑сервер, который подключается к вашему проекту 1С:Предприятие и предоставляет ассистенту команды для сборки и запуска тестов YaXUnit.
 
## Быстрый старт

1. Соберите проект:
   ```bash
   ./gradlew build
   ```
2. Подготовьте конфигурацию приложения: см. подробный гид — [Application Configuration](docs/APPLICATION_CONFIGURATION.md).
3. Запустите MCP‑сервер (пример):
   ```bash
   export SPRING_CONFIG_IMPORT=/abs/path/to/application.yml
   java -jar /path/to/mcp-yaxunit-runner.jar
   ```

## Основные возможности

- Запуск всех тестов проекта
- Выполнение тестов отдельных модулей
- Сборка проекта 1С
- Получение списка доступных модулей
- Проверка статуса платформы 1С
- Получение конфигурации проекта

```mermaid
flowchart LR
    subgraph " "
        A["🚀 Запрос на<br/>выполнение тестов"]
        B["🔍 Анализ<br/>изменений"]
        C{"📊 Есть<br/>изменения?"}
        D["🔨 Сборка<br/>проекта"]
        E["🧪 Запуск<br/>тестов"]
        F{"✅ Сборка<br/>успешна?"}
        G["❌ Ошибка<br/>сборки"]
        H["📋 Результат<br/>тестирования"]
    end
    
    A --> B
    B --> C
    C -->|Да| D
    C -->|Нет| E
    D --> F
    F -->|Да| E
    F -->|Нет| G
    E --> H
    
    %% Современные стили с градиентами и тенями
    classDef startNode fill:#4FC3F7,stroke:#0277BD,stroke-width:3px,color:#fff,font-weight:bold
    classDef processNode fill:#81C784,stroke:#388E3C,stroke-width:3px,color:#fff,font-weight:bold
    classDef decisionNode fill:#FFB74D,stroke:#F57C00,stroke-width:3px,color:#fff,font-weight:bold
    classDef successNode fill:#A5D6A7,stroke:#4CAF50,stroke-width:3px,color:#fff,font-weight:bold
    classDef errorNode fill:#EF5350,stroke:#C62828,stroke-width:3px,color:#fff,font-weight:bold
    
    class A startNode
    class B,D,E processNode
    class C,F decisionNode
    class H successNode
    class G errorNode
```

## Дорожная карта разработки 🚀

- [ ] 🔄 Поддержка EDT + умная конвертация
  - [ ] **Интеграция с EDT (Enterprise Development Tools)**
  - [ ] **Умная конвертация модулей** - при изменении модуля автоматическое копирование и обновление
  - [ ] **Автоматическое определение изменений** в исходном коде

- [ ] 🖥️ Поддержка IBMCMD
  - **Интеграция с IBMCMD** для автоматизации сборки

- [ ] 🌐 Запуск тестов через WebSocket
  - **Быстрое выполнение** тестов

## Технические требования

- JDK 17+
- Gradle 8.5+
- Платформа 1С:Предприятие 8.3.10+
- YaXUnit фреймворк
- 1С:Enterprise Development Tools 2025.1+

## Установка и сборка

### Сборка проекта
```bash
./gradlew build
```

### Создание исполняемого JAR
```bash
./gradlew bootJar
```

## Запуск MCP-сервера
```bash
java -jar mcp-yaxunit-runner.jar
```

## Настройка MCP

### Предварительные требования

1. **Соберите проект** и создайте JAR файл
2. **Убедитесь, что JAR файл доступен** для запуска
3. **Создайте файл конфигурации** для вашего проекта

### Создание файла конфигурации

Перед настройкой MCP сервера создайте файл конфигурации для вашего проекта. Файл `src/main/resources/application-yaxunit.yml` является примером — скопируйте его и настройте под свои нужды.

Полный пошаговый гид по настройке: [Application Configuration](docs/APPLICATION_CONFIGURATION.md).

#### Основные параметры для настройки:

- **`app.base-path`** - базовый путь к вашему проекту
- **`app.source-set`** - описание модулей проекта (пути, типы, назначение)
- **`app.connection.connection-string`** - строка подключения к информационной базе
- **`app.platform-version`** - версия платформы 1С
 - **`app.tools.builder`** - тип сборщика (DESIGNER или IBMCMD)

#### Настройка исходников тестов:

Для корректной работы с тестами YaXUnit обязательно настройте в `source-set`:

```yaml
source-set:
  # Основная конфигурация (обязательно)
  - path: "configuration"
    name: your-config-name
    type: "CONFIGURATION"
    purpose: [ "MAIN" ]
  
  # Модуль с тестами
  - path: "tests"
    name: tests
    type: "EXTENSION"
    purpose: [ "TESTS", "YAXUNIT" ]
```

#### Пример структуры:
```yaml
app:
  id: your-project-name
  base-path: "/path/to/your/project/"
  source-set:
    - path: "configuration"
      name: your-config
      type: "CONFIGURATION"
      purpose: ["TESTS"]
  connection:
    connection-string: "File='/path/to/your/infobase/';"
  platform-version: "8.3.24.1234"
  tools:
    builder: DESIGNER
```

### Настройка MCP сервера

#### 1. Базовая конфигурация

```json
{
  "mcpServers": {
    "yaxunit-runner": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/path/to/your/mcp-yaxunit-runner.jar"
      ],
      "env": {
        "LOGGING_LEVEL_ROOT": "DEBUG",
        "SPRING_CONFIG_IMPORT": "/path/to/your/application.yml"
      }
    }
  }
}
```

#### 2. Настройка для Claude Desktop

Добавьте конфигурацию в файл `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "yaxunit-runner": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/path/to/your/mcp-yaxunit-runner.jar"
      ],
      "env": {
        "LOGGING_LEVEL_ROOT": "INFO",
        "SPRING_CONFIG_IMPORT": "/path/to/your/application.yml"
      }
    }
  }
}
```

#### 3. Настройка для VS Code

Создайте файл `.vscode/settings.json` в вашем проекте:

```json
{
  "mcp.servers": {
    "yaxunit-runner": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "${workspaceFolder}/build/libs/mcp-yaxunit-runner.jar"
      ],
      "env": {
        "LOGGING_LEVEL_ROOT": "DEBUG",
        "SPRING_CONFIG_IMPORT": "${workspaceFolder}/src/main/resources/application-yaxunit.yml"
      }
    }
  }
}
```

### Проверка настройки

1. **Тест подключения** - запустите MCP сервер вручную для проверки
2. **Проверка логов** - убедитесь, что сервер запускается без ошибок
3. **Тест команд** - в AI-ассистенте попробуйте выполнить простую команду

### Устранение неполадок

- **JAR файл не найден** - проверьте путь и убедитесь, что проект собран
- **Ошибки прав доступа** - проверьте права на выполнение JAR файла
- **Проблемы подключения** - проверьте логи, порты и настройки файрвола

### Переменные окружения

Основные переменные для настройки:
- `LOGGING_LEVEL_ROOT` - уровень логирования
- `SPRING_CONFIG_IMPORT` - путь к файлу конфигурации
- `JAVA_OPTS` - дополнительные опции Java

## Интеграция с AI-ассистентами

После настройки MCP сервера, вы можете использовать его с различными AI-ассистентами:

### Claude Desktop
- Добавьте конфигурацию MCP в `claude_desktop_config.json`
- Перезапустите Claude Desktop
- Проверьте подключение через команду: "Покажи доступные MCP инструменты"

### VS Code
- Установите расширение MCP для VS Code
- Настройте сервер в `.vscode/settings.json`
- Используйте AI-ассистента встроенного в VS Code

### Другие MCP-совместимые клиенты
- Cursor
- Continue
- Cody
- И другие клиенты, поддерживающие протокол MCP

### Примеры использования

После успешной настройки вы сможете:

1. **Запускать тесты** через AI-ассистента:
   ```
   Запусти все тесты проекта YaXUnit
   ```

2. **Анализировать результаты** тестирования:
   ```
   Покажи результаты последнего запуска тестов
   ```

3. **Собирать проекты** 1С:
   ```
   Собери проект с конфигурацией demo
   ```

4. **Получать информацию** о модулях:
   ```
   Какие модули доступны в проекте?
   ```

## Доступные MCP-инструменты

- `yaxunit_run_all_tests` - запуск всех тестов
- `yaxunit_run_module_tests` - запуск тестов модуля
- `yaxunit_build_project` - сборка проекта
- `yaxunit_list_modules` - получение списка модулей
- `yaxunit_get_configuration` - получение конфигурации
- `yaxunit_check_platform` - проверка платформы

## Разработка

### Запуск тестов
```bash
./gradlew test
```

### Анализ покрытия кода
```bash
./gradlew jacocoTestReport
```

### Проверка стиля кода
```bash
./gradlew ktlintCheck
```

## Документация

- [Application Configuration](docs/APPLICATION_CONFIGURATION.md) — подробный пошаговый гид по настройке `application.yml`

## Лицензия

MIT License 