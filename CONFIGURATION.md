# MCP YaXUnit Runner - Документация по настройке

## Обзор

MCP YaXUnit Runner поддерживает гибкую систему конфигурации, которая позволяет настраивать приложение для различных сред выполнения (разработка, тестирование, продакшн).

## Файлы конфигурации

### Основные файлы

1. **`src/main/resources/application.yml`** - Основная конфигурация по умолчанию
2. **`src/main/resources/application-dev.yml`** - Конфигурация для среды разработки
3. **`src/main/resources/application-prod.yml`** - Конфигурация для продакшн среды
4. **`src/main/resources/application-test.yml`** - Конфигурация для тестовой среды
5. **`src/main/resources/application.properties`** - Переменные окружения Spring Boot
6. **`env.example`** - Пример файла переменных окружения
7. **`docker-compose.yml`** - Конфигурация Docker Compose

## Профили Spring Boot

### Активация профилей

```bash
# Через переменную окружения
export SPRING_PROFILES_ACTIVE=dev

# Через параметр командной строки
java -jar mcp-yaxunit-runner.jar --spring.profiles.active=prod

# Через Docker
docker run -e SPRING_PROFILES_ACTIVE=dev mcp-yaxunit-runner:latest
```

### Доступные профили

- **`default`** - Профиль по умолчанию
- **`dev`** - Среда разработки
- **`test`** - Тестовая среда
- **`prod`** - Продакшн среда

## Основные настройки

### Настройки проекта

```yaml
project:
  tests_path: "./tests"           # Путь к тестам
  build_timeout: 300000          # Таймаут сборки (мс)
  test_timeout: 600000           # Таймаут выполнения тестов (мс)
```

### Настройки платформы 1С

```yaml
platform:
  version: null                  # Версия платформы (null = автоопределение)
  auto_detect_version: true      # Автоматическое определение версии
  search_paths:                  # Пути поиска платформы
    - "/opt/1cv8"
    - "/usr/local/1cv8"
```

### Настройки информационной базы

```yaml
information_base:
  connection: null               # Строка подключения к ИБ
  user: null                     # Пользователь ИБ
  password: null                 # Пароль ИБ
```

### Настройки сборки

```yaml
build:
  incremental_enabled: true      # Инкрементальная сборка
  hash_storage_size: 10000       # Размер хранилища хешей
  parallel_build: true           # Параллельная сборка
```

### Настройки логирования

```yaml
logging:
  level: INFO                    # Уровень логирования
  enable_file_logging: false     # Логирование в файл
  file_path: null                # Путь к файлу логов
```

### Настройки сервера

```yaml
server:
  port: 8080                     # Порт сервера
  enable_websocket: true         # WebSocket поддержка
  websocket_path: "/yaxunit"     # Путь WebSocket
```

## Переменные окружения

### Основные переменные

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `SPRING_PROFILES_ACTIVE` | Активный профиль | `default` |
| `YAXUNIT_TESTS_PATH` | Путь к тестам | `./tests` |
| `YAXUNIT_BUILD_TIMEOUT` | Таймаут сборки | `300000` |
| `YAXUNIT_TEST_TIMEOUT` | Таймаут тестов | `600000` |
| `YAXUNIT_LOG_LEVEL` | Уровень логирования | `INFO` |
| `YAXUNIT_SERVER_PORT` | Порт сервера | `8080` |

### Переменные платформы

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `YAXUNIT_PLATFORM_VERSION` | Версия платформы | `null` |
| `YAXUNIT_AUTO_DETECT` | Автоопределение версии | `true` |
| `YAXUNIT_PLATFORM_PATHS` | Пути поиска платформы | Системные пути |

### Переменные информационной базы

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `IB_CONNECTION` | Строка подключения | `null` |
| `IB_USER` | Пользователь | `null` |
| `IB_PWD` | Пароль | `null` |

## Docker конфигурация

### Запуск с Docker Compose

```bash
# Продакшн
docker-compose up

# Разработка
docker-compose --profile dev up

# Тестирование
docker-compose --profile test up

# С мониторингом
docker-compose --profile monitoring up
```

### Переменные окружения в Docker

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - YAXUNIT_TESTS_PATH=/opt/yaxunit/tests
  - YAXUNIT_BUILD_TIMEOUT=180000
  - YAXUNIT_LOG_LEVEL=WARN
```

## Мониторинг и метрики

### Endpoints управления

- **Health Check**: `http://localhost:8080/actuator/health`
- **Info**: `http://localhost:8080/actuator/info`
- **Metrics**: `http://localhost:8080/actuator/metrics`

### Prometheus метрики

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

## Безопасность

### CORS настройки

```yaml
security:
  enabled: false
  cors:
    allowed_origins: "*"
    allowed_methods: "GET,POST,PUT,DELETE,OPTIONS"
    allowed_headers: "*"
```

## Примеры конфигурации

### Локальная разработка

```bash
# Копируем пример переменных окружения
cp env.example .env

# Редактируем .env файл
nano .env

# Запускаем с профилем разработки
java -jar mcp-yaxunit-runner.jar --spring.profiles.active=dev
```

### Продакшн развертывание

```bash
# Создаем конфигурацию для продакшн
cat > application-prod.yml << EOF
spring:
  application:
    name: mcp-yaxunit-runner-prod

project:
  tests_path: "/opt/yaxunit/tests"
  build_timeout: 180000
  test_timeout: 300000

logging:
  level: WARN
  enable_file_logging: true
  file_path: "/var/log/yaxunit/app.log"
EOF

# Запускаем с продакшн профилем
java -jar mcp-yaxunit-runner.jar --spring.profiles.active=prod
```

### Docker развертывание

```bash
# Собираем образ
docker build -t mcp-yaxunit-runner .

# Запускаем контейнер
docker run -d \
  --name mcp-yaxunit-runner \
  -p 8080:8080 \
  -v /opt/yaxunit/tests:/opt/yaxunit/tests:ro \
  -v /var/log/yaxunit:/var/log/yaxunit \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e YAXUNIT_TESTS_PATH=/opt/yaxunit/tests \
  mcp-yaxunit-runner:latest
```

## Устранение неполадок

### Проверка конфигурации

```bash
# Проверяем активный профиль
curl http://localhost:8080/actuator/info

# Проверяем health status
curl http://localhost:8080/actuator/health

# Просматриваем логи
tail -f /var/log/yaxunit/app.log
```

### Частые проблемы

1. **Платформа 1С не найдена**
   - Проверьте пути в `YAXUNIT_PLATFORM_PATHS`
   - Убедитесь, что платформа установлена

2. **Таймауты**
   - Увеличьте `YAXUNIT_BUILD_TIMEOUT` и `YAXUNIT_TEST_TIMEOUT`
   - Проверьте производительность системы

3. **Проблемы с WebSocket**
   - Проверьте настройки `YAXUNIT_WEBSOCKET_ENABLED`
   - Убедитесь, что порт не занят

## Дополнительные ресурсы

- [Spring Boot Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Prometheus Configuration](https://prometheus.io/docs/prometheus/latest/configuration/configuration/) 