# 🎨 CREATIVE DESIGN: YaXUnit Test Runner Implementation

## 🎯 DESIGN OBJECTIVES
Создать надежную и расширяемую систему запуска тестов YaXUnit, которая интегрируется с существующей архитектурой MCP и поддерживает все описанные в документации способы запуска тестов с фокусом на jUnit XML отчетах.

## 🏗️ ARCHITECTURAL DECISIONS

### 1. Core Components Design

#### YaXUnitTestAction
```kotlin
class YaXUnitTestAction(
    private val platformUtilityDsl: PlatformUtilityDsl,
    private val utilLocator: CrossPlatformUtilLocator,
    private val configWriter: JsonYaXUnitConfigWriter,
    private val reportParser: EnhancedReportParser
) : RunTestAction
```

**Key Responsibilities:**
- Координация процесса запуска тестов
- Создание конфигурации запуска
- Локализация утилиты ENTERPRISE
- Обработка результатов и jUnit XML отчетов

#### ProcessYaXUnitRunner
```kotlin
class ProcessYaXUnitRunner(
    private val utilLocator: CrossPlatformUtilLocator,
    private val configWriter: JsonYaXUnitConfigWriter
) : YaXUnitRunner
```

**Key Responsibilities:**
- Формирование команды запуска 1С:Предприятие
- Выполнение процесса тестирования
- Обработка выходных данных
- Управление временными файлами

### 2. Configuration System Design

#### JsonYaXUnitConfigWriter Enhancements
```kotlin
data class YaXUnitConfig(
    val filter: TestFilter? = null,
    val reportFormat: ReportFormat = ReportFormat.JUNIT_XML,
    val reportPath: String? = null,
    val closeAfterTests: Boolean = true,
    val showReport: Boolean = false,
    val logging: LoggingConfig? = null,
    val externalControl: ExternalControlConfig? = null
)

data class TestFilter(
    val modules: List<String>? = null,
    val extensions: List<String>? = null,
    val tests: List<String>? = null,
    val tags: List<String>? = null,
    val contexts: List<String>? = null
)

data class LoggingConfig(
    val file: String? = null,
    val enable: Boolean? = null,
    val console: Boolean = false,
    val level: String = "debug"
)
```

### 3. Enterprise Integration Design

#### Command Line Formation
```kotlin
// Пример команды запуска согласно документации:
// "C:\Program Files\1cv8\8.3.18.1698\bin\1cv8c.exe" ENTERPRISE /IBName MyInfoBase /N Admin /C RunUnitTests=C:\tmp\test-config.json

private fun buildEnterpriseCommandArgs(
    utilityLocation: UtilityLocation,
    request: TestExecutionRequest,
    configPath: Path
): List<String> {
    return listOf(
        utilityLocation.executablePath.toString(),
        "ENTERPRISE",
        *buildConnectionArgs(request),
        "/C",
        "RunUnitTests=${configPath.toAbsolutePath()}"
    )
}
```

### 4. jUnit Report Processing Design

#### EnhancedReportParser Integration
```kotlin
interface EnhancedReportParser : ReportParser {
    suspend fun parseJUnitReport(input: InputStream): GenericTestReport
    suspend fun detectReportFormat(input: InputStream): ReportFormat
}

// jUnit XML Report Structure
data class JUnitTestSuite(
    val name: String,
    val tests: Int,
    val failures: Int,
    val errors: Int,
    val skipped: Int,
    val time: Double,
    val testCases: List<JUnitTestCase>
)

data class JUnitTestCase(
    val name: String,
    val className: String,
    val time: Double,
    val status: TestStatus,
    val failure: JUnitFailure? = null,
    val systemOut: String? = null,
    val systemErr: String? = null
)

data class JUnitFailure(
    val message: String,
    val type: String,
    val details: String
)
```

## 🔧 TECHNICAL IMPLEMENTATION PLAN

### Phase 1: Core Infrastructure (CURRENT FOCUS)

#### 1.1 Enhance ProcessYaXUnitRunner
- [ ] Реализовать `buildEnterpriseCommandArgs()` с поддержкой всех типов подключения
- [ ] Добавить обработку различных форматов строк подключения (File=, IBConnectionString)
- [ ] Реализовать `executeProcess()` с детальным логированием
- [ ] Добавить `determineReportPath()` с поддержкой jUnit XML формата

#### 1.2 Improve JsonYaXUnitConfigWriter
- [ ] Создать data classes для конфигурации (YaXUnitConfig, TestFilter, LoggingConfig)
- [ ] Реализовать поддержку всех параметров конфигурации
- [ ] Добавить валидацию конфигурации
- [ ] Поддержка jUnit XML формата отчетов

#### 1.3 Update YaXUnitTestAction
- [ ] Интегрировать с улучшенным ProcessYaXUnitRunner
- [ ] Добавить поддержку различных типов запросов (RunAllTestsRequest, RunModuleTestsRequest, RunListTestsRequest)
- [ ] Реализовать обработку jUnit XML отчетов через EnhancedReportParser
- [ ] Добавить детальное логирование процесса

### Phase 2: Platform Integration

#### 2.1 CrossPlatformUtilLocator Enhancement
- [ ] Убедиться, что ENTERPRISE utility type поддерживается
- [ ] Добавить специфичные пути поиска для 1cv8c.exe
- [ ] Реализовать валидацию ENTERPRISE utility
- [ ] Добавить кэширование путей к ENTERPRISE

#### 2.2 Platform-Specific Adaptations
- [ ] Windows: поддержка путей C:\Program Files\1cv8\
- [ ] Linux: поддержка путей /opt/1cv8/
- [ ] Добавить определение версии платформы
- [ ] Реализовать fallback механизмы

### Phase 3: Configuration & Reporting

#### 3.1 Configuration Management
- [ ] Поддержка всех параметров фильтрации (modules, extensions, tests, tags, contexts)
- [ ] Реализация LoggingConfig с поддержкой файлов и консоли
- [ ] Добавить ExternalControlConfig для интеграции с EDT
- [ ] Валидация конфигурации перед запуском

#### 3.2 jUnit Report Processing
- [ ] Реализовать EnhancedReportParser для jUnit XML формата
- [ ] Поддержка стандартного jUnit XML schema
- [ ] Автоматическое определение jUnit XML отчета
- [ ] Преобразование в GenericTestReport

### Phase 4: Error Handling & Logging

#### 4.1 Comprehensive Error Handling
- [ ] Обработка ошибок запуска процесса
- [ ] Валидация конфигурации
- [ ] Обработка ошибок парсинга jUnit XML отчетов
- [ ] Graceful degradation при отсутствии отчетов

#### 4.2 Detailed Logging
- [ ] Логирование процесса формирования команды
- [ ] Логирование выполнения процесса
- [ ] Логирование обработки jUnit XML результатов
- [ ] Поддержка различных уровней логирования

## 🎯 SUCCESS METRICS

### Functional Requirements
- ✅ Запуск тестов через 1С:Предприятие с параметром RunUnitTests
- ✅ Поддержка всех параметров конфигурации из документации
- ✅ Обработка jUnit XML отчетов
- ✅ Интеграция с ENTERPRISE utility type
- ✅ Корректная обработка ошибок

### Quality Requirements
- ✅ 100% покрытие тестами новых компонентов
- ✅ Документация API и примеры использования
- ✅ Поддержка Windows и Linux платформ
- ✅ Производительность: запуск тестов < 30 секунд

### Integration Requirements
- ✅ Совместимость с существующей архитектурой MCP
- ✅ Интеграция с ActionFactory
- ✅ Поддержка всех типов TestExecutionRequest
- ✅ Расширяемость для будущих форматов отчетов

## 📝 IMPLEMENTATION NOTES

### Key Design Principles
1. **Separation of Concerns** - каждый компонент отвечает за свою область
2. **Dependency Injection** - использование Spring для управления зависимостями
3. **Error Resilience** - graceful handling ошибок на всех уровнях
4. **Platform Independence** - поддержка различных операционных систем
5. **jUnit XML Focus** - специализация на jUnit XML формате отчетов

### Integration Points
- **ActionFactory** - регистрация YaXUnitTestAction
- **CrossPlatformUtilLocator** - поиск ENTERPRISE utility
- **EnhancedReportParser** - обработка jUnit XML отчетов
- **ApplicationProperties** - конфигурация приложения

### Testing Strategy
- **Unit Tests** - для каждого компонента отдельно
- **Integration Tests** - тестирование взаимодействия компонентов
- **End-to-End Tests** - полный цикл запуска тестов
- **Platform Tests** - тестирование на различных ОС
- **jUnit XML Tests** - тестирование парсинга jUnit XML отчетов
