# Настройка MCP сервера в IDE

После создания файла конфигурации (см. [Application Configuration](APPLICATION_CONFIGURATION.md)), настройте MCP сервер в вашем IDE или AI-ассистенте.

## Содержание

- [Содержание](#содержание)
- [Конфигурация](#конфигурация)
  - [Описание параметров](#описание-параметров)
- [Проверка настройки](#проверка-настройки)
- [Переменные окружения](#переменные-окружения)
  - [LOGGING\_LEVEL\_ROOT](#logging_level_root)
  - [SPRING\_CONFIG\_IMPORT](#spring_config_import)
  - [JAVA\_OPTS](#java_opts)
- [Дополнительная информация](#дополнительная-информация)

## Конфигурация

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

### Описание параметров

- **`type`**: тип подключения (обычно "stdio" для стандартного ввода/вывода)
- **`command`**: команда для запуска JVM
- **`args`**: аргументы командной строки
  - `-jar`: запуск JAR файла
  - путь к JAR файлу
- **`env`**: переменные окружения
  - `LOGGING_LEVEL_ROOT`: уровень логирования (DEBUG, INFO, WARN, ERROR)
  - `SPRING_CONFIG_IMPORT`: путь к файлу конфигурации приложения

## Проверка настройки

В AI-ассистенте попробуйте выполнить простую команду:

```plaintext
Покажи доступные MCP инструменты
```

Или попросите ассистента:

```plaintext
Запусти все тесты проекта
```

## Переменные окружения

Основные переменные для настройки:

### LOGGING_LEVEL_ROOT

Уровень логирования для приложения.

**Возможные значения**:

- `DEBUG` - детальная отладочная информация
- `INFO` - общая информация о работе
- `WARN` - предупреждения
- `ERROR` - только ошибки

**Пример**:

```bash
LOGGING_LEVEL_ROOT=INFO
```

### SPRING_CONFIG_IMPORT

Путь к файлу конфигурации приложения.

**Пример**:

```bash
SPRING_CONFIG_IMPORT=/path/to/application-yaxunit.yml
```

### JAVA_OPTS

Дополнительные опции для JVM.

**Пример**:

```bash
JAVA_OPTS=-Xmx2048m -Xms1024m
```

Используйте эту переменную для:

- Настройки размера кучи JVM
- Включения отладки
- Настройки других параметров JVM

## Дополнительная информация

- [Application Configuration](APPLICATION_CONFIGURATION.md) - подробный гид по настройке конфигурации
- [README](../README.md) - основная документация проекта

