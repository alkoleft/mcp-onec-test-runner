# DSL для работы с утилитами платформы 1С:Предприятие

## Обзор

DSL (Domain Specific Language) для работы с утилитами платформы 1С:Предприятие предоставляет удобный и типобезопасный интерфейс для выполнения
операций с конфигуратором и инфобазным менеджером (ibcmd).

## Архитектура

```
dsl/
├── PlatformUtilityDsl.kt              # Основной DSL класс
├── common/
│   └── PlatformUtilityContext.kt      # Общий контекст для утилит
├── configurator/
│   └── ConfiguratorDsl.kt            # DSL для конфигуратора
├── ibcmd/
│   └── IbcmdDsl.kt                   # DSL для инфобазного менеджера
├── executor/
│   └── ProcessExecutor.kt             # Исполнитель процессов
└── examples/
    └── PlatformUtilityExamples.kt     # Примеры использования
```

## Основные возможности

### 1. Конфигуратор (1cv8)

DSL для работы с конфигуратором предоставляет следующие операции:

#### Загрузка конфигурации

- **LoadConfigFromFiles** - Загрузка конфигурации из каталога с файлами исходников
- **LoadCfg** - Загрузка основной конфигурации или расширения из файла

#### Проверка и обновление

- **CheckCanApplyConfigurationExtensions** - Проверка возможности применения расширений
- **UpdateDBCfg** - Обновление конфигурации в информационной базе
- **CheckConfig** - Проверка конфигурации
- **CheckModules** - Проверка модулей конфигурации

#### Произвольные команды

- Выполнение любых команд конфигуратора через метод `command()`

### 2. Инфобазный менеджер (ibcmd)

DSL для работы с инфобазным менеджером предоставляет следующие операции:

- **CREATEINFOBASE** - Создание информационной базы
- **DROPINFOBASE** - Удаление информационной базы
- **COPYINFOBASE** - Копирование информационной базы
- **RESTOREINFOBASE** - Восстановление информационной базы
- **COMPRESSINFOBASE** - Сжатие информационной базы
- **REINDEXINFOBASE** - Реиндексация информационной базы
- **CHECKINFOBASE** - Проверка информационной базы
- **UPDATEINFOBASE** - Обновление информационной базы
- **LISTINFOBASES** - Получение списка информационных баз
- **INFOBASE** - Получение информации об информационной базе
- **Произвольные команды** - Выполнение любых команд ibcmd

## Использование

### Базовое использование

```kotlin
// Создание DSL
val platformDsl = PlatformUtilityDsl(utilLocator)

// Работа с конфигуратором
val configResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
    output(Paths.get("/path/to/output.txt"))
    log(Paths.get("/path/to/log.txt"))
}.loadFromFiles(Paths.get("/path/to/source/files"))

// Работа с инфобазным менеджером
val ibResult = platformDsl.ibcmd("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
    output(Paths.get("/path/to/output.txt"))
}.create()
```

### Параметры конфигурации

#### Общие параметры

- `connect(connectionString)` - Строка подключения к информационной базе
- `user(username)` - Имя пользователя
- `password(password)` - Пароль пользователя
- `output(path)` - Путь для вывода результатов
- `log(path)` - Путь для лог-файла
- `param(parameter)` - Дополнительные параметры

#### Специфичные для конфигуратора

- `config(path)` - Путь к файлу конфигурации (для операций с .cf файлами)
- `language(code)` - Код языка интерфейса
- `localization(code)` - Код локализации сеанса
- `connectionSpeed(speed)` - Скорость соединения (NORMAL/LOW)
- `disableStartupDialogs()` - Отключение диалоговых окон запуска
- `disableStartupMessages()` - Отключение сообщений запуска
- `noTruncate()` - Не очищать файл вывода при записи

### Команды конфигуратора

```kotlin
// Загрузка конфигурации из каталога с файлами исходников
val loadFromFilesResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
    output(Paths.get("/path/to/output.txt"))
    log(Paths.get("/path/to/log.txt"))
    language("ru")
    localization("ru")
    connectionSpeed(ConnectionSpeed.NORMAL)
    disableStartupDialogs()
    disableStartupMessages()
    noTruncate()
}.loadFromFiles(Paths.get("/path/to/source/files"))

// Загрузка основной конфигурации из файла
val loadMainResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
    output(Paths.get("/path/to/main_config.log"))
}.loadMainConfig(Paths.get("/path/to/main_config.cf"))

// Загрузка расширения конфигурации
val loadExtensionResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
    output(Paths.get("/path/to/extension.log"))
}.loadExtension(Paths.get("/path/to/extension.cfe"))

// Проверка возможности применения расширений
val checkExtensionsResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
}.checkCanApplyExtensions()

// Обновление конфигурации в информационной базе
val updateResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
    output(Paths.get("/path/to/update.log"))
}.updateDatabaseConfig()

// Проверка конфигурации
val checkConfigResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
    output(Paths.get("/path/to/check_config.log"))
}.checkConfig()

// Проверка модулей конфигурации
val checkModulesResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
    output(Paths.get("/path/to/check_modules.log"))
}.checkModules()

// Произвольная команда
val customResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
}.command("DumpCfg")
```

### Команды инфобазного менеджера

```kotlin
// Создание информационной базы
val createResult = platformDsl.ibcmd("8.3.24.1482") {
    connect("Srvr=localhost;Ref=NewDatabase;")
    user("Administrator")
    password("password")
}.create()

// Получение списка информационных баз
val listResult = platformDsl.ibcmd("8.3.24.1482") {
    connect("Srvr=localhost;")
    user("Administrator")
    password("password")
}.list()

// Проверка информационной базы
val checkResult = platformDsl.ibcmd("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
}.check()

// Сжатие информационной базы
val compressResult = platformDsl.ibcmd("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
}.compress()

// Произвольная команда
val customResult = platformDsl.ibcmd("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
}.command("LOCKINFOBASE")
```

### Комплексная работа с конфигурацией

```kotlin
// 1. Загружаем основную конфигурацию
val mainConfigResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
    output(Paths.get("/path/to/main_config.log"))
    disableStartupDialogs()
}.loadMainConfig(Paths.get("/path/to/main_config.cf"))

if (mainConfigResult.success) {
    println("Основная конфигурация загружена успешно")

    // 2. Загружаем расширение
    val extensionResult = platformDsl.configurator("8.3.24.1482") {
        connect("Srvr=localhost;Ref=MyDatabase;")
        user("Administrator")
        password("password")
        output(Paths.get("/path/to/extension.log"))
    }.loadExtension(Paths.get("/path/to/extension.cfe"))

    if (extensionResult.success) {
        println("Расширение загружено успешно")

        // 3. Проверяем возможность применения расширений
        val checkExtensionsResult = platformDsl.configurator("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
        }.checkCanApplyExtensions()

        if (checkExtensionsResult.success) {
            println("Расширения можно применить")

            // 4. Обновляем конфигурацию в базе
            val updateResult = platformDsl.configurator("8.3.24.1482") {
                connect("Srvr=localhost;Ref=MyDatabase;")
                user("Administrator")
                password("password")
                output(Paths.get("/path/to/update.log"))
            }.updateDatabaseConfig()

            if (updateResult.success) {
                println("Конфигурация обновлена успешно")

                // 5. Проверяем конфигурацию
                val checkResult = platformDsl.configurator("8.3.24.1482") {
                    connect("Srvr=localhost;Ref=MyDatabase;")
                    user("Administrator")
                    password("password")
                    output(Paths.get("/path/to/final_check.log"))
                }.checkConfig()

                if (checkResult.success) {
                    println("Конфигурация проверена успешно")
                } else {
                    println("Ошибка проверки конфигурации: ${checkResult.error}")
                }
            } else {
                println("Ошибка обновления конфигурации: ${updateResult.error}")
            }
        } else {
            println("Ошибка проверки расширений: ${checkExtensionsResult.error}")
        }
    } else {
        println("Ошибка загрузки расширения: ${extensionResult.error}")
    }
} else {
    println("Ошибка загрузки основной конфигурации: ${mainConfigResult.error}")
}
```

### Обработка результатов

```kotlin
val result = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
}.loadFromFiles(Paths.get("/path/to/source/files"))

if (result.success) {
    println("Операция выполнена успешно")
    println("Вывод: ${result.output}")
    println("Длительность: ${result.duration}")
} else {
    println("Ошибка: ${result.error}")
    println("Код выхода: ${result.exitCode}")
}
```

### Обработка ошибок

```kotlin
try {
    val result = platformDsl.configurator("8.3.24.1482") {
        connect("Srvr=invalid;Ref=invalid;")
        user("Administrator")
        password("password")
    }.loadFromFiles(Paths.get("/invalid/path"))

    if (!result.success) {
        println("Ошибка выполнения: ${result.error}")
    }

} catch (e: Exception) {
    println("Исключение: ${e.message}")
}
```

### Синхронное использование

```kotlin
// Синхронная проверка платформы
val platformResult = platformDsl.platformSync("8.3.24.1482") {
    // Синхронная проверка доступности утилит
}

// Синхронная команда конфигуратора
val configResult = platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
}.commandSync("CheckConfig")
```

## Особенности реализации

### 1. Автоматическое обнаружение утилит

DSL автоматически обнаруживает утилиты платформы 1С с помощью `CrossPlatformUtilLocator`:

```kotlin
// Автоматический поиск утилит
val location = context.locateUtility(UtilityType.DESIGNER)
```

### 2. Асинхронное выполнение

Все операции выполняются асинхронно с поддержкой корутин:

```kotlin
suspend fun executeConfiguratorCommand(command: String): ConfiguratorResult {
    // Асинхронное выполнение команды
}
```

### 3. Обработка таймаутов

Поддержка таймаутов для длительных операций:

```kotlin
suspend fun executeWithTimeout(
    commandArgs: List<String>,
    timeoutMs: Long = 300000 // 5 минут по умолчанию
): ProcessResult
```

### 4. Кросс-платформенность

Поддержка Windows и Linux платформ:

```kotlin
// Автоматическое определение платформы
val platformType = platformDetector.current
```

## Тестирование

### Unit тесты

```kotlin
@Test
fun `should create configurator DSL with version`() {
    // Given
    val version = "8.3.24.1482"
    val mockLocation = UtilityLocation(...)

    coEvery { mockUtilLocator.locateUtility(UtilityType.DESIGNER, version) } returns mockLocation

    // When
    val result = platformDsl.configurator(version) {
        connect("Srvr=localhost;Ref=TestDB;")
        user("Administrator")
        password("password")
    }.buildResult()

    // Then
    assertTrue(result.success)
    assertEquals(0, result.exitCode)
}
```

### Интеграционные тесты

```kotlin
@Test
fun `should handle real configurator command`() {
    // Given
    val platformDsl = PlatformUtilityDsl(realUtilLocator)

    // When
    val result = platformDsl.configurator("8.3.24.1482") {
        connect("Srvr=localhost;Ref=TestDB;")
        user("Administrator")
        password("password")
    }.checkConfig()

    // Then
    assertTrue(result.success)
}
```

## Преимущества DSL

### 1. Типобезопасность

Все операции типобезопасны и проверяются на этапе компиляции:

```kotlin
// Компилятор проверит правильность использования
platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
}.loadFromFiles(Paths.get("/path/to/source/files")) // Возвращает ConfiguratorResult
```

### 2. Fluent API

Удобный fluent API для настройки параметров:

```kotlin
platformDsl.configurator("8.3.24.1482") {
    connect("Srvr=localhost;Ref=MyDatabase;")
    user("Administrator")
    password("password")
    output(Paths.get("/path/to/output.txt"))
    log(Paths.get("/path/to/log.txt"))
    language("ru")
    localization("ru")
    connectionSpeed(ConnectionSpeed.NORMAL)
    disableStartupDialogs()
    disableStartupMessages()
    noTruncate()
}.loadFromFiles(Paths.get("/path/to/source/files"))
```

### 3. Обработка ошибок

Комплексная обработка ошибок с детальной информацией:

```kotlin
data class ConfiguratorResult(
    val success: Boolean,
    val output: String,
    val error: String?,
    val exitCode: Int,
    val duration: Duration
)
```

### 4. Расширяемость

Легкое добавление новых команд и параметров:

```kotlin
// Добавление новой команды
fun customCommand(): ConfiguratorResult {
    return executeConfiguratorCommand("CUSTOMCOMMAND")
}

// Добавление нового параметра
fun customParam(value: String) {
    additionalParams.add("/CustomParam$value")
}
```

## Заключение

DSL для работы с утилитами платформы 1С:Предприятие предоставляет:

- **Удобный интерфейс** для работы с конфигуратором и инфобазным менеджером
- **Типобезопасность** на этапе компиляции
- **Асинхронное выполнение** с поддержкой корутин
- **Кросс-платформенность** для Windows и Linux
- **Комплексную обработку ошибок** с детальной информацией
- **Легкую расширяемость** для добавления новых команд и параметров
- **Поддержку всех основных операций** конфигуратора и инфобазного менеджера

DSL значительно упрощает работу с утилитами платформы 1С и делает код более читаемым и поддерживаемым. 