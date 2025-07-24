# YaXUnit Runner MCP Server

## Быстрый старт

### 1. Сборка проекта

```bash
./gradlew clean build
```

### 2. Запуск MCP сервера

```bash
# Запуск как MCP сервер через STDIO
java -jar build/libs/mcp-yaxunit-runner-1.0-SNAPSHOT.jar --spring.profiles.active=mcp

# Запуск с дополнительными профилями
java -jar build/libs/mcp-yaxunit-runner-1.0-SNAPSHOT.jar --spring.profiles.active=mcp,dev
```

### 3. Интеграция с Claude Desktop

#### macOS
Добавьте в `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "yaxunit-runner": {
    "command": "java",
    "args": [
      "-jar",
      "/path/to/mcp-yaxunit-runner-1.0-SNAPSHOT.jar",
      "--spring.profiles.active=mcp"
    ],
    "env": {
      "YAXUNIT_TESTS_PATH": "./tests",
      "YAXUNIT_BUILD_TIMEOUT": "300000",
      "YAXUNIT_TEST_TIMEOUT": "600000"
    }
  }
}
```

#### Windows
Добавьте в `%APPDATA%\Claude\claude_desktop_config.json`:

```json
{
  "yaxunit-runner": {
    "command": "java",
    "args": [
      "-jar",
      "C:\\path\\to\\mcp-yaxunit-runner-1.0-SNAPSHOT.jar",
      "--spring.profiles.active=mcp"
    ],
    "env": {
      "YAXUNIT_TESTS_PATH": ".\\tests",
      "YAXUNIT_BUILD_TIMEOUT": "300000",
      "YAXUNIT_TEST_TIMEOUT": "600000"
    }
  }
}
```

### 4. Тестирование

После настройки перезапустите Claude Desktop и попробуйте:

```
Запусти все тесты YaXUnit в проекте
```

## Доступные команды

### Тестирование
- `yaxunit_run_all_tests` - Запуск всех тестов
- `yaxunit_run_module_tests` - Запуск тестов модуля
- `yaxunit_run_list_tests` - Запуск тестов из списка модулей

### Управление проектом
- `yaxunit_build_project` - Сборка проекта
- `yaxunit_list_modules` - Список модулей
- `yaxunit_get_configuration` - Конфигурация проекта
- `yaxunit_check_platform` - Статус платформы 1С

## Примеры использования

### Запуск всех тестов
```
Запусти все тесты YaXUnit в проекте и покажи результаты
```

### Тестирование конкретного модуля
```
Запусти тесты YaXUnit для модуля 'Accounting' и покажи результаты
```

### Проверка статуса платформы
```
Проверь статус платформы 1С для выполнения тестов YaXUnit
```

### Получение конфигурации
```
Покажи текущую конфигурацию проекта YaXUnit
```

## Конфигурация

### Переменные окружения

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `YAXUNIT_TESTS_PATH` | Путь к тестам | `./tests` |
| `YAXUNIT_BUILD_TIMEOUT` | Таймаут сборки | `300000` |
| `YAXUNIT_TEST_TIMEOUT` | Таймаут тестов | `600000` |
| `YAXUNIT_LOG_LEVEL` | Уровень логирования | `INFO` |
| `YAXUNIT_AUTO_DETECT` | Автоопределение версии | `true` |

### Профили

- `mcp` - Основной профиль MCP сервера
- `dev` - Профиль разработки
- `prod` - Продакшн профиль
- `test` - Тестовый профиль

## Docker

### Запуск в Docker

```bash
# Сборка образа
docker build -t mcp-yaxunit-runner .

# Запуск контейнера
docker run -d \
  --name yaxunit-mcp-server \
  -v /opt/yaxunit/tests:/opt/yaxunit/tests:ro \
  -e SPRING_PROFILES_ACTIVE=mcp \
  -e YAXUNIT_TESTS_PATH=/opt/yaxunit/tests \
  mcp-yaxunit-runner:latest
```

### Docker Compose

```bash
# Запуск с Docker Compose
docker-compose --profile mcp up -d
```

## Устранение неполадок

### MCP сервер не запускается

1. Проверьте Java версию (требуется Java 17+)
2. Убедитесь, что JAR файл собран корректно
3. Проверьте логи на наличие ошибок

### Claude не видит инструменты

1. Проверьте конфигурацию Claude Desktop
2. Убедитесь, что MCP сервер запущен
3. Перезапустите Claude Desktop

### Ошибки при выполнении тестов

1. Проверьте доступность платформы 1С
2. Убедитесь в корректности путей к тестам
3. Проверьте права доступа к файлам

## Разработка

### Добавление новых инструментов

```kotlin
@Tool(
    name = "yaxunit_custom_tool",
    description = "Описание нового инструмента"
)
fun customTool(
    @ToolParameter(description = "Описание параметра") parameter: String
): CustomResult {
    // Реализация инструмента
    return CustomResult()
}
```

### Локальная разработка

```bash
# Запуск с профилем разработки
./gradlew bootRun --args='--spring.profiles.active=mcp,dev'

# Запуск примера
./gradlew bootRun --args='--spring.profiles.active=mcp --mcp.example.enabled=true'
```

## Документация

- [MCP Integration Guide](MCP_INTEGRATION.md)
- [Configuration Guide](CONFIGURATION.md)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Model Context Protocol](https://modelcontextprotocol.io/)

## Лицензия

MIT License 