### Как подготовить файл настроек за 10 минут

Этот гид поможет быстро подготовить рабочий `application.yml` для MCP YaXUnit Runner. Начните с готового шаблона, заполните 4 поля и запустите проверку.

### Что понадобится

- **Java 17+** и собранный JAR приложения
- **Абсолютный путь** к корню вашего проекта 1С
- **Структура исходников**: где лежат конфигурация, тесты, YaXUnit
- **Строка подключения** к вашей ИБ (файловой или серверной)

### Шаг 1. Скопируйте шаблон и заполните плейсхолдеры

```yaml
app:
  # (необязательно) Человекочитаемый идентификатор проекта
  id: your-project

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

### Быстрый чек‑лист

- `app.base-path` — абсолютный путь существует и читается.
- `source-set` — пути уникальны, относительны `base-path`, на диске существуют.
- Есть хотя бы один `CONFIGURATION`.
- `connection.connection-string` содержит `=` и валидна для вашей ИБ.
- `tools.builder` — одно из: `DESIGNER`, `IBMCMD`.

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

### Что приложение проверяет автоматически

- Существование и читаемость `base-path`.
- Непустой `source-set`, уникальные пути, наличие `CONFIGURATION`.
- Существование путей `source-set` относительно `base-path`.
- Корректность строки подключения (наличие `=`).
- Формат версии платформы, если указана: `x.x.x.x`.
- Допустимость значений перечислений (`type`, `purpose`, `builder`).

### Частые ошибки и как их исправить

- «Base path does not exist» — проверьте абсолютный путь в `app.base-path`.
- «Source set path does not exist» — путь в `source-set` не найден относительно `base-path`.
- «Connection string must contain '=' character» — исправьте `connection-string`.
- «Invalid builder type» — используйте `DESIGNER` или `IBMCMD` в `app.tools.builder`.
- `tools-properties` вместо `tools` — используйте корректный ключ `app.tools`.

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
  platform-version: string?      # обязателен, формат x.x.x.x
```

