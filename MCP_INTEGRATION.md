# MCP Integration Guide - YaXUnit Runner

## Обзор

MCP (Model Context Protocol) интеграция позволяет использовать YaXUnit Runner как MCP сервер для AI моделей, таких как Claude, через Spring AI. Это обеспечивает стандартизированный способ взаимодействия AI моделей с системой тестирования YaXUnit.

## Архитектура MCP

### Компоненты

1. **MCP Server** - YaXUnit Runner как MCP сервер
2. **MCP Client** - Spring AI клиент для подключения к серверу
3. **AI Model** - Модель AI (Claude, GPT и др.)
4. **Tools** - Инструменты для выполнения операций

### Поток данных

```
AI Model → MCP Client → MCP Server → YaXUnit Services → 1C Platform
```

## Доступные MCP Tools

### 1. `yaxunit_run_all_tests`
Запускает все тесты YaXUnit в проекте.

**Описание:** Запускает все тесты YaXUnit в проекте. Возвращает подробный отчет о выполнении тестов.

**Возвращает:** `TestExecutionResult`

### 2. `yaxunit_run_module_tests`
Запускает тесты из указанного модуля.

**Параметры:**
- `moduleName` (String) - Имя модуля для тестирования

**Возвращает:** `TestExecutionResult`

### 3. `yaxunit_run_list_tests`
Запускает тесты из списка указанных модулей.

**Параметры:**
- `moduleNames` (List<String>) - Список имен модулей для тестирования

**Возвращает:** `TestExecutionResult`

### 4. `yaxunit_build_project`
Выполняет сборку проекта.

**Возвращает:** `BuildResult`

### 5. `yaxunit_list_modules`
Возвращает список доступных модулей.

**Возвращает:** `ModuleListResult`

### 6. `yaxunit_get_configuration`
Возвращает текущую конфигурацию проекта.

**Возвращает:** `ConfigurationResult`

### 7. `yaxunit_check_platform`
Проверяет статус платформы 1С.

**Возвращает:** `PlatformStatusResult`

## Настройка MCP Server

### 1. Зависимости

Добавьте зависимости Spring AI MCP в `build.gradle.kts`:

```kotlin
// Spring AI MCP Server
implementation("org.springframework.ai:spring-ai-mcp-server-spring-boot-starter")
implementation("org.springframework.ai:spring-ai-mcp-server-stdio-spring-boot-starter")
implementation("org.springframework.ai:spring-ai-mcp-server-http-spring-boot-starter")
implementation("org.springframework.ai:spring-ai-mcp-server-websocket-spring-boot-starter")
```

### 2. Конфигурация

Используйте профиль `mcp` для запуска как MCP сервер:

```bash
java -jar mcp-yaxunit-runner.jar --spring.profiles.active=mcp
```

### 3. Конфигурация для Claude Desktop

Добавьте в `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS):

```json
{
  "yaxunit-runner": {
    "command": "java",
    "args": [
      "-jar",
      "/path/to/mcp-yaxunit-runner.jar",
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

Для Windows (`%APPDATA%\Claude\claude_desktop_config.json`):

```json
{
  "yaxunit-runner": {
    "command": "java",
    "args": [
      "-jar",
      "C:\\path\\to\\mcp-yaxunit-runner.jar",
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

## Использование с Spring AI

### 1. Создание MCP клиента

```kotlin
@Service
class McpClientService {
    fun createMcpClient(serverName: String = "yaxunit-runner"): McpClient {
        return McpClientBuilder.builder()
            .name(serverName)
            .version("1.0.0")
            .description("MCP client for YaXUnit Runner")
            .build()
    }
}
```

### 2. Интеграция с Chat клиентом

```kotlin
fun createChatClientWithTools(mcpClient: McpClient): ChatClient {
    return ChatClient.builder()
        .defaultTools(mcpClient)
        .build()
}
```

### 3. Выполнение запросов

```kotlin
fun executeWithMcpTools(prompt: String): ChatResponse {
    val mcpClient = createMcpClient()
    val chatClient = createChatClientWithTools(mcpClient)
    return chatClient.prompt(prompt).call()
}
```

## Примеры использования

### Пример 1: Запуск всех тестов

```kotlin
val response = mcpClientService.executeWithMcpTools(
    "Запусти все тесты YaXUnit в проекте и покажи результаты"
)
println(response.content)
```

### Пример 2: Тестирование конкретного модуля

```kotlin
val response = mcpClientService.executeWithMcpTools(
    "Запусти тесты YaXUnit для модуля 'Accounting' и покажи результаты"
)
println(response.content)
```

### Пример 3: Проверка статуса платформы

```kotlin
val response = mcpClientService.executeWithMcpTools(
    "Проверь статус платформы 1С для выполнения тестов YaXUnit"
)
println(response.content)
```

### Пример 4: Получение конфигурации

```kotlin
val response = mcpClientService.executeWithMcpTools(
    "Покажи текущую конфигурацию проекта YaXUnit"
)
println(response.content)
```

## Docker интеграция

### Запуск MCP сервера в Docker

```bash
docker run -d \
  --name yaxunit-mcp-server \
  -v /opt/yaxunit/tests:/opt/yaxunit/tests:ro \
  -e SPRING_PROFILES_ACTIVE=mcp \
  -e YAXUNIT_TESTS_PATH=/opt/yaxunit/tests \
  mcp-yaxunit-runner:latest
```

### Docker Compose

```yaml
services:
  yaxunit-mcp-server:
    image: mcp-yaxunit-runner:latest
    container_name: yaxunit-mcp-server
    environment:
      - SPRING_PROFILES_ACTIVE=mcp
      - YAXUNIT_TESTS_PATH=/opt/yaxunit/tests
    volumes:
      - ./tests:/opt/yaxunit/tests:ro
    profiles:
      - mcp
```

## Мониторинг и отладка

### Логирование

```yaml
logging:
  level:
    io.github.alkoleft.mcp: DEBUG
    org.springframework.ai: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

### Проверка статуса

```bash
# Проверка работы MCP сервера
curl -X POST http://localhost:8080/mcp/status

# Просмотр логов
tail -f /var/log/yaxunit/mcp.log
```

## Устранение неполадок

### Проблема: MCP сервер не запускается

**Решение:**
1. Проверьте зависимости в `build.gradle.kts`
2. Убедитесь, что профиль `mcp` активен
3. Проверьте логи на наличие ошибок

### Проблема: AI модель не видит инструменты

**Решение:**
1. Проверьте конфигурацию Claude Desktop
2. Убедитесь, что MCP сервер запущен
3. Перезапустите Claude Desktop

### Проблема: Ошибки при выполнении тестов

**Решение:**
1. Проверьте доступность платформы 1С
2. Убедитесь в корректности путей к тестам
3. Проверьте права доступа к файлам

## Расширение функциональности

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

### Кастомные типы данных

```kotlin
data class CustomResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any>
)
```

## Ресурсы

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [Claude Desktop MCP Guide](https://docs.anthropic.com/claude/docs/model-context-protocol-mcp)
- [Spring AI GitHub Repository](https://github.com/spring-projects/spring-ai)

## Примеры кода

Полные примеры использования доступны в:
- `src/main/kotlin/io/github/alkoleft/mcp/examples/McpExample.kt`
- `src/main/kotlin/io/github/alkoleft/mcp/application/services/McpClientService.kt`
- `src/main/kotlin/io/github/alkoleft/mcp/interfaces/mcp/YaXUnitMcpServer.kt` 