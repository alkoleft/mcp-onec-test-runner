# IBCMD DSL - Предметно-ориентированный язык для работы с ibcmd

## Обзор

IBCMD DSL предоставляет удобный Kotlin-интерфейс для работы с утилитой `ibcmd` 1С:Предприятия 8. DSL спроектирован по принципам SOLID с декомпозицией
команд по отдельным файлам и поддерживает все режимы работы ibcmd.

## Архитектура

### Структура проекта

```
ibcmd/
├── commands/
│   ├── common/                    # Общие интерфейсы и параметры
│   │   ├── IbcmdCommand.kt       # Базовый интерфейс команд
│   │   ├── CommonParameters.kt   # Общие параметры для всех команд
│   │   └── CommandBuilders.kt    # Builder'ы для создания команд
│   ├── infobase/                 # Команды управления информационной базой
│   │   ├── InfobaseCommands.kt
│   │   └── InfobaseExtensionCommands.kt
│   ├── server/                   # Команды настройки автономного сервера
│   │   └── ServerCommands.kt
│   ├── config/                   # Команды работы с конфигурациями
│   │   ├── ConfigCommands.kt
│   │   ├── ConfigExportImportCommands.kt
│   │   └── ConfigExtensionCommands.kt
│   ├── session/                  # Команды администрирования сеансов
│   │   └── SessionCommands.kt
│   ├── lock/                     # Команды администрирования блокировок
│   │   └── LockCommands.kt
│   ├── mobile/                   # Команды работы с мобильными приложениями
│   │   ├── MobileAppCommands.kt
│   │   └── MobileClientCommands.kt
│   └── extension/                # Команды работы с расширениями
│       └── ExtensionCommands.kt
├── IbcmdCommands.kt              # Агрегатор команд и константы
├── IbcmdDsl.kt                   # Основной DSL класс
├── IbcmdPlan.kt                  # План выполнения команд
├── IbcmdContext.kt               # Контекст выполнения
├── IbcmdResult.kt                # Результат выполнения
└── README.md                     # Документация
```

### Принципы SOLID

1. **Single Responsibility Principle (SRP)**: Каждый файл команд отвечает за конкретный режим работы ibcmd
2. **Open/Closed Principle (OCP)**: Архитектура позволяет добавлять новые команды без изменения существующих
3. **Liskov Substitution Principle (LSP)**: Все команды реализуют общий интерфейс `IbcmdCommand`
4. **Interface Segregation Principle (ISP)**: Параметры разделены на логические группы
5. **Dependency Inversion Principle (DIP)**: DSL зависит от абстракций, а не от конкретных реализаций

## Поддерживаемые режимы и команды

### 1. infobase — Управление информационной базой

- `create` — Создание информационной базы
- `dump` — Выгрузка данных ИБ
- `restore` — Загрузка данных ИБ
- `clear` — Очистка ИБ
- `replicate` — Репликация ИБ
- `extension` — Управление расширениями в ИБ
- `generation-id` — Идентификатор поколения конфигурации
- `sign` — Цифровая подпись конфигурации/расширения

### 2. server — Настройка автономного сервера

- `config init` — Инициализация конфигурации автономного сервера
- `config import` — Импорт конфигурации из кластера серверов 1С

### 3. config — Работа с конфигурациями и расширениями

- `load` — Загрузка конфигурации
- `save` — Выгрузка конфигурации
- `check` — Проверка конфигурации
- `apply` — Обновление конфигурации БД
- `reset` — Возврат к конфигурации БД
- `repair` — Восстановление после незавершённой операции
- `export` — Экспорт конфигурации в XML
- `import` — Импорт конфигурации из XML
- `support disable` — Снятие конфигурации с поддержки
- `data-separation list` — Список разделителей ИБ
- `extension` — Управление расширениями конфигурации
- `generation-id` — Идентификатор поколения конфигурации
- `sign` — Цифровая подпись конфигурации/расширения

### 4. session — Администрирование сеансов

- `info` — Получение информации о сеансе
- `list` — Получение списка сеансов
- `terminate` — Принудительное завершение сеанса
- `interrupt-current-server-call` — Прерывание текущего серверного вызова

### 5. lock — Администрирование блокировок

- `list` — Получение списка блокировок

### 6. mobile-app — Работа с мобильным приложением

- `export` — Экспорт мобильного приложения

### 7. mobile-client — Работа с мобильным клиентом

- `export` — Экспорт мобильного клиента
- `sign` — Цифровая подпись мобильного клиента

### 8. extension — Работа с расширениями

- `create` — Создание расширения
- `info` — Получение информации о расширении
- `list` — Получение списка расширений
- `update` — Обновление свойств расширения
- `delete` — Удаление расширения

## Примеры использования

### Создание информационной базы

```kotlin
val plan = platformDsl.ibcmdPlan("8.3.24.1761") {
    infobase {
        create {
            dbParams = DatabaseConnectionParameters(
                databasePath = "/path/to/infobase"
            )
            locale = "ru"
            createDatabase = true
        }
    }
}

plan.execute()
```

### Загрузка и применение конфигурации

```kotlin
val plan = platformDsl.ibcmdPlan("8.3.24.1761") {
    // Общие параметры для всех команд
    dbPath("/path/to/infobase")
    user("Админ")
    password("123")

    config {
        // Загрузка конфигурации
        load("/path/to/config.cf") {
            force = true
        }

        // Применение изменений
        apply {
            dynamic = DynamicUpdateMode.AUTO.value
            sessionTerminate = SessionTerminateMode.FORCE.value
        }

        // Проверка конфигурации
        check()
    }
}

plan.execute()
```

### Работа с расширениями

```kotlin
val plan = platformDsl.ibcmdPlan("8.3.24.1761") {
    dbPath("/path/to/infobase")
    user("Админ")
    password("123")

    extension {
        // Создание расширения
        create("MyExtension", "ME") {
            purpose = ExtensionPurpose.ADD_ON.value
            synonym = "ru='Мое расширение'"
        }

        // Активация расширения
        update("MyExtension") {
            active = IbcmdBoolean.YES.value
            safeMode = IbcmdBoolean.YES.value
            scope = ExtensionScope.INFOBASE.value
        }

        // Получение информации
        info("MyExtension")
    }
}

plan.execute()
```

### Администрирование сеансов

```kotlin
val plan = platformDsl.ibcmdPlan("8.3.24.1761") {
    session {
        // Получение списка сеансов
        list {
            licenses = true
        }

        // Завершение конкретного сеанса
        terminate("12345678-1234-1234-1234-123456789012") {
            errorMessage = "Плановое завершение для обслуживания"
        }
    }
}

plan.execute()
```

### Экспорт/импорт конфигурации в XML

```kotlin
val plan = platformDsl.ibcmdPlan("8.3.24.1761") {
    dbPath("/path/to/infobase")
    user("Админ")
    password("123")

    config {
        // Экспорт в XML
        export("/path/to/xml/export") {
            exportSubCommand = "objects"
            threads = 4
            archive = true
            sync = true
        }

        // Импорт из XML
        import("/path/to/xml/import") {
            importSubCommand = "files"
            partial = true
            noCheck = false
        }
    }
}

plan.execute()
```

## Типы данных и перечисления

DSL предоставляет типизированные перечисления для всех параметров:

- `ExtensionPurpose` — назначение расширения (customization, add-on, patch)
- `IbcmdBoolean` — булевы значения (yes, no)
- `DynamicUpdateMode` — режимы динамического обновления
- `SessionTerminateMode` — режимы завершения сеансов
- `ExtensionScope` — области действия расширений
- `DbmsType` — типы СУБД
- `ServerFlag` — флаги для автономного сервера

## Обработка ошибок

Все команды возвращают объект `IbcmdResult` с информацией о выполнении:

```kotlin
val results = plan.execute()
results.forEach { result ->
    if (!result.success) {
        println("Ошибка выполнения: ${result.error}")
        println("Код выхода: ${result.exitCode}")
    } else {
        println("Команда выполнена успешно")
        println("Вывод: ${result.output}")
    }
}
```

## Расширение функциональности

Для добавления новых команд:

1. Создайте data class, реализующий `IbcmdCommand`
2. Добавьте команду в соответствующий файл режима
3. Обновите builder'ы при необходимости
4. Добавьте константы в `IbcmdCommands`

Пример:

```kotlin
data class NewCommand(
    val commonParams: CommonParameters = CommonParameters(),
    val specificParam: String
) : IbcmdCommand {
    override val mode: String = "new-mode"
    override val subCommand: String = "new-command"
    override val commandName: String = "new-mode new-command"

    override val arguments: List<String>
        get() = commonParams.toArguments() + listOf("--specific", specificParam)

    override fun getFullDescription(): String = "New command with param: $specificParam"
}
``` 