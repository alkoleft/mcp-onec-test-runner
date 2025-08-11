### Как подготовить файл настроек за 10 минут

Этот гид поможет быстро подготовить рабочий `application.yml` для MCP YaXUnit Runner. Начните с готового шаблона, заполните 4–5 полей и запустите проверку.

### Что понадобится

- **Java 17+** и собранный JAR приложения
- **Абсолютный путь** к корню вашего проекта 1С
- **Структура исходников**: где лежат конфигурация, тесты, YaXUnit
- **Строка подключения** к вашей ИБ (файловой или серверной)
- Если используете EDT: **1C:EDT 2025.1+ (минимальная версия)**. Обоснование см. [Issue #1758](https://github.com/1C-Company/1c-edt-issues/issues/1758).

### Шаг 1. Скопируйте шаблон и заполните плейсхолдеры

```yaml
app:
  # (необязательно) Человекочитаемый идентификатор проекта
  id: your-project

  # Формат проекта: DESIGNER (типовой) или EDT (структура EDT)
  format: DESIGNER

  # Абсолютный путь к корню проекта 1С на диске
  base-path: "/abs/path/to/project"

  # Описание исходников проекта (пути относительно base-path)
  source-set:
    - path: "configuration"
      name: main
      type: "CONFIGURATION"
      purpose: ["MAIN"]

  # Подключение к информационной базе
  connection:
    # Примеры ниже; оставьте ровно одну строку
    connection-string: "File='/abs/path/to/infobase/';"
    # user: "Администратор"     # (необязательно)
    # password: "secret"        # (необязательно)

  # Инструменты сборки/запуска
  tools:
    # DESIGNER — локальная разработка, IBMCMD — CI/CD
    builder: DESIGNER

  # Версия платформы
  platform-version: "8.3.24.1234"
```

### Шаг 2. Опишите исходники под ваш проект

Выберите один из типовых вариантов и скорректируйте пути/назначения.

- **Только конфигурация**
  ```yaml
  source-set:
    - path: "configuration"
      name: main
      type: "CONFIGURATION"
      purpose: ["MAIN"]
  ```

- **Конфигурация + тесты (расширение)**
  ```yaml
  source-set:
    - path: "configuration"
      name: main
      type: "CONFIGURATION"
      purpose: ["MAIN"]
    - path: "tests"
      name: tests
      type: "EXTENSION"
      purpose: ["TESTS"]
  ```

- **Конфигурация + YaXUnit engine + тесты**
  ```yaml
  source-set:
    - path: "configuration"
      name: demo
      type: "CONFIGURATION"
      purpose: ["MAIN", "TESTS"]
    - path: "yaxunit"
      name: yaxunit
      type: "EXTENSION"
      purpose: ["YAXUNIT"]
    - path: "tests"
      name: tests
      type: "EXTENSION"
      purpose: ["TESTS", "YAXUNIT"]
  ```

Требования к `source-set`:
- Должен быть хотя бы один элемент с `type: CONFIGURATION`.
- Пути уникальны, относительны `base-path` и существуют на диске.
- Путь не должен начинаться с `/` и содержать `..`.
- Имя `name` не должно содержать разделители путей (`/` или `\`).

### Шаг 3. Заполните подключение к ИБ

Укажите ровно одну строку подключения — она обязательно должна содержать знак `=`.

- **Файловая база**
  ```yaml
  connection:
    connection-string: "File='/abs/path/to/infobase/';"
  ```

- **Серверная база**
  ```yaml
  connection:
    connection-string: "Srvr='server';Ref='infobase';"
    # user: "Администратор"
    # password: "secret"
  ```

Поля `user` и `password` — опциональны.

### Шаг 4. Выберите тип сборщика

- **DESIGNER** — удобно для локальной разработки.
- **IBMCMD** — надежно для CI/CD и headless-сборок.

Укажите в `app.tools.builder`: `DESIGNER` или `IBMCMD`.

### Шаг 5. (опционально) Включите поддержку EDT CLI

Если ваш проект в формате EDT, укажите `app.format: EDT`. Для автозапуска и управления интерактивной сессией EDT CLI настройте блок `tools.edt-cli`.

Требуется версия 1C:EDT не ниже 2025.1 из-за известных проблем экспорта/работы из существующей рабочей области в предыдущих версиях (см. [Issue #1758](https://github.com/1C-Company/1c-edt-issues/issues/1758)).

```yaml
app:
  format: EDT
  tools:
    builder: DESIGNER
    edt-cli:
      auto-start: true                 # автозапуск при старте приложения (по умолчанию: false)
      version: "latest"               # версия EDT CLI (по умолчанию: latest; требуется EDT >= 2025.1)
      interactive-mode: true           # интерактивный режим (по умолчанию: true)
      working-directory: "/abs/path/to/edt-workspace"  # рабочая папка EDT
      startup-timeout-ms: 30000        # ожидание готовности промпта (по умолчанию: 30000)
      command-timeout-ms: 300000       # таймаут команд (по умолчанию: 300000)
      ready-check-timeout-ms: 5000     # таймаут проверки готовности (по умолчанию: 5000)
```

### Быстрый чек‑лист

- `app.base-path` — абсолютный путь существует и читается.
- `app.format` — одно из: `DESIGNER`, `EDT`.
- `source-set` — пути уникальны, относительны `base-path`, на диске существуют.
- Есть хотя бы один `CONFIGURATION`.
- `connection.connection-string` содержит `=` и валидна для вашей ИБ.
- `tools.builder` — одно из: `DESIGNER`, `IBMCMD`.
- Для EDT при необходимости настроен `tools.edt-cli`.

### Готовые примеры

- **Минимальный**
  ```yaml
  app:
    base-path: "/abs/path/to/project"
    source-set:
      - path: "configuration"
        name: main
        type: "CONFIGURATION"
        purpose: ["MAIN"]
    connection:
      connection-string: "File='/abs/path/to/infobase/';"
    tools:
      builder: DESIGNER
  ```

- **С тестами**
  ```yaml
  app:
    id: "my-project"
    base-path: "/abs/path/to/project"
    source-set:
      - path: "configuration"
        name: main
        type: "CONFIGURATION"
        purpose: ["MAIN"]
      - path: "tests"
        name: tests
        type: "EXTENSION"
        purpose: ["TESTS"]
    connection:
      connection-string: "File='/abs/path/to/infobase/';"
      user: "Администратор"
      password: "password123"
    platform-version: "8.3.24.1234"
    tools:
      builder: DESIGNER
  ```

- **Серверная ИБ + IBMCMD**
  ```yaml
  app:
    id: "auth-project"
    base-path: "/abs/path/to/project"
    source-set:
      - path: "configuration"
        name: main
        type: "CONFIGURATION"
        purpose: ["MAIN"]
    connection:
      connection-string: "Srvr='server';Ref='infobase';"
      user: "Tester"
      password: "secret"
    tools:
      builder: IBMCMD
  ```

- **EDT проект с автозапуском EDT CLI**
  ```yaml
  app:
    id: "edt-project"
    format: EDT
    base-path: "/abs/path/to/edt-project"
    source-set:
      - path: "fixtures/demo-configuration"
        name: configuration
        type: "CONFIGURATION"
        purpose: ["TESTS"]
      - path: "exts/yaxunit"
        name: yaxunit
        type: "EXTENSION"
        purpose: ["MAIN", "YAXUNIT"]
      - path: "tests"
        name: tests
        type: "EXTENSION"
        purpose: ["TESTS"]
    connection:
      connection-string: "File='/abs/path/to/infobase/';"
    platform-version: "8.3.22.1709"
    tools:
      builder: DESIGNER
      edt-cli:
        auto-start: true
        working-directory: "/abs/path/to/edt-workspace"
        startup-timeout-ms: 300000
        command-timeout-ms: 600000
        ready-check-timeout-ms: 5000
  ```

### Что приложение проверяет автоматически

- Существование и читаемость `base-path`.
- Непустой `source-set`, уникальные пути, наличие `CONFIGURATION`.
- Существование путей `source-set` относительно `base-path`.
- Корректность строки подключения (наличие `=`).
- Формат версии платформы, если указана: `x[.x]+` (например, `8.3.22.1709`).
- Допустимость значений перечислений (`format`, `type`, `purpose`, `builder`).
- Корректность элементов `source-set`: путь относительный, без `..`; имя без разделителей путей.

### Частые ошибки и как их исправить

- «Base path does not exist» — проверьте абсолютный путь в `app.base-path`.
- «Source set path does not exist» — путь в `source-set` не найден относительно `base-path`.
- «Connection string must contain '=' character» — исправьте `connection-string`.
- «Invalid builder type» — используйте `DESIGNER` или `IBMCMD` в `app.tools.builder`.
- `tools-properties` вместо `tools` — используйте корректный ключ `app.tools`.
- «Invalid project format» — используйте `DESIGNER` или `EDT` в `app.format`.
- «Source set path must be relative / cannot contain '..'» — укажите относительный путь без `..`.
- «Source set name cannot contain path separators» — имя не должно содержать `/` или `\`.

### Как запустить с вашим конфигом

Укажите путь к файлу настроек через переменную окружения и запустите сервер:

```bash
export SPRING_CONFIG_IMPORT=/abs/path/to/your/application.yml
export SPRING_PROFILES_ACTIVE=mcp
java -jar /abs/path/to/mcp-yaxunit-runner.jar mcp
```

### Справочник параметров (кратко)

```yaml
app:
  id: string?                    # опционально
  format: DESIGNER|EDT           # опционально (по умолчанию DESIGNER)
  base-path: string              # обязателен, абсолютный путь
  source-set:                    # обязателен, >=1 элемент с type: CONFIGURATION
    - path: string               # относительный путь от base-path
      name: string               # уникальное имя
      type: CONFIGURATION|EXTENSION
      purpose: [ MAIN | TESTS | YAXUNIT ]
  connection:
    connection-string: string    # обязателен
    user: string?                # опционально
    password: string?            # опционально
  tools:
    builder: DESIGNER|IBMCMD     # обязателен
    edt-work-space: string?      # опционально (рабочая папка EDT, при необходимости)
    edt-cli:                     # опционально, для EDT
      auto-start: boolean        # по умолчанию false
      version: string            # по умолчанию "latest" (минимум EDT 2025.1)
      interactive-mode: boolean  # по умолчанию true
      working-directory: string? # рабочая папка EDT workspace
      startup-timeout-ms: number # по умолчанию 30000
      command-timeout-ms: number # по умолчанию 300000
      ready-check-timeout-ms: number # по умолчанию 5000
  platform-version: string?      # опционально, формат x[.x]+ (например, 8.3.22.1709)
```

