# Creative Design: Actions Architecture

## 🎨🎨🎨 ENTERING CREATIVE PHASE: ARCHITECTURE DESIGN

### Компонент: Actions Architecture
**Описание:** Система Actions является основой для выполнения различных задач в проекте MCP YaxUnit Runner. Actions предоставляют абстракцию для сборки проектов, анализа изменений и запуска тестов.

### Требования и ограничения:

#### Функциональные требования:
1. **BuildAction** - сборка конфигурации и расширений через Designer или ibcmd
2. **ChangeAnalysisAction** - анализ изменений в файловой системе
3. **RunTestAction** - запуск тестов с поддержкой фильтрации
4. **Составные назначения** - SourceSetPurpose с множественными значениями
5. **Интеграция с существующими сервисами** - BuildOrchestrationService, TestLauncherService
6. **Поддержка различных стратегий сборки** - Designer vs ibcmd

#### Качественные требования:
1. **Тестируемость** - покрытие тестами > 80%
2. **Расширяемость** - легко добавлять новые типы Actions
3. **Производительность** - асинхронное выполнение через coroutines
4. **Надежность** - специфичные исключения для каждого типа Action
5. **Логирование** - подробное логирование для отладки

#### Технические ограничения:
1. **Kotlin 2.1.20** с JDK 17
2. **Spring Boot 3.5.3** для DI
3. **Существующая архитектура** - интеграция с PlatformUtilityDsl
4. **Обратная совместимость** - не требуется

### Анализ вариантов архитектуры:

#### Вариант 1: Простая фабрика с прямыми реализациями
**Преимущества:**
- Простота реализации
- Прямая связь с существующими сервисами
- Минимальные изменения в архитектуре
- Быстрое внедрение

**Недостатки:**
- Ограниченная расширяемость
- Сложность тестирования (сильные зависимости)
- Отсутствие абстракции для общих операций
- Дублирование кода между реализациями

#### Вариант 2: Стратегия с контекстом и декораторами
**Преимущества:**
- Высокая расширяемость
- Легкое тестирование с моками
- Возможность добавления cross-cutting concerns
- Четкое разделение ответственности
- Поддержка композиции действий

**Недостатки:**
- Увеличенная сложность
- Больше файлов и классов
- Потенциальная избыточность для простых случаев
- Сложность понимания для новых разработчиков

#### Вариант 3: Event-driven с Command Pattern
**Преимущества:**
- Полная декомпозиция операций
- Возможность асинхронной обработки
- Легкое добавление middleware
- Поддержка сложных workflow
- Отслеживание состояния операций

**Недостатки:**
- Высокая сложность реализации
- Избыточность для простых операций
- Сложность отладки
- Потенциальные проблемы с производительностью

#### Вариант 4: Гибридный подход с модульной архитектурой
**Преимущества:**
- Баланс между простотой и расширяемостью
- Модульная структура
- Легкое тестирование
- Четкое разделение по доменам
- Возможность постепенного развития

**Недостатки:**
- Умеренная сложность
- Необходимость тщательного планирования интерфейсов
- Потенциальное дублирование общих операций

### Рекомендуемый подход: Вариант 4 - Гибридный подход

**Обоснование выбора:**
1. **Соответствие требованиям** - покрывает все функциональные требования
2. **Баланс сложности** - не избыточен для текущих потребностей
3. **Расширяемость** - легко добавлять новые модули
4. **Тестируемость** - четкое разделение ответственности
5. **Интеграция** - хорошо сочетается с существующей архитектурой

### Руководство по реализации:

#### 1. Структура файлов:
```
src/main/kotlin/io/github/alkoleft/mcp/application/actions/
├── core/
│   ├── Action.kt (обновленные интерфейсы)
│   ├── ActionExecutor.kt (новый)
│   └── ActionRegistry.kt (новый)
├── modules/
│   ├── build/
│   │   ├── BuildAction.kt
│   │   ├── DesignerBuildAction.kt
│   │   └── IbcmdBuildAction.kt
│   ├── change/
│   │   ├── ChangeAnalysisAction.kt
│   │   └── FileSystemChangeAnalysisAction.kt
│   └── test/
│       ├── RunTestAction.kt
│       └── YaXUnitTestAction.kt
├── common/
│   ├── ActionContext.kt (новый)
│   ├── ActionResult.kt (новый)
│   └── exceptions/
│       └── ActionExceptions.kt (обновленный)
└── ActionService.kt (новый)
```

#### 2. Ключевые интерфейсы:
```kotlin
// Базовый интерфейс для всех Actions
interface Action<T : ActionContext, R : ActionResult> {
    suspend fun execute(context: T): R
}

// Контекст выполнения
data class ActionContext(
    val projectProperties: ProjectProperties,
    val applicationProperties: ApplicationProperties,
    val metadata: Map<String, Any> = emptyMap()
)

// Базовый результат
sealed class ActionResult {
    data class Success<T>(val data: T) : ActionResult()
    data class Failure(val error: String, val exception: Throwable? = null) : ActionResult()
}

// Специфичные интерфейсы
interface BuildAction : Action<BuildContext, BuildResult>
interface ChangeAnalysisAction : Action<ChangeContext, ChangeAnalysisResult>
interface RunTestAction : Action<TestContext, TestExecutionResult>
```

#### 3. Реализация модулей:
```kotlin
// Модуль сборки
@Service
class DesignerBuildAction(
    private val platformUtilityDsl: PlatformUtilityDsl
) : BuildAction {
    override suspend fun execute(context: BuildContext): BuildResult {
        // Реализация через PlatformUtilityDsl
    }
}

// Модуль анализа изменений
@Service
class FileSystemChangeAnalysisAction(
    private val fileWatcher: FileWatcher
) : ChangeAnalysisAction {
    override suspend fun execute(context: ChangeContext): ChangeAnalysisResult {
        // Реализация анализа файловой системы
    }
}

// Модуль тестирования
@Service
class YaXUnitTestAction(
    private val platformUtilityDsl: PlatformUtilityDsl
) : RunTestAction {
    override suspend fun execute(context: TestContext): TestExecutionResult {
        // Реализация запуска тестов (draft)
    }
}
```

#### 4. Реестр и выполнение:
```kotlin
@Service
class ActionRegistry(
    private val buildActions: List<BuildAction>,
    private val changeActions: List<ChangeAnalysisAction>,
    private val testActions: List<RunTestAction>
) {
    fun getBuildAction(type: BuilderType): BuildAction {
        return when (type) {
            BuilderType.DESIGNER -> buildActions.find { it is DesignerBuildAction }
            BuilderType.IBMCMD -> buildActions.find { it is IbcmdBuildAction }
        } ?: throw IllegalArgumentException("Unknown builder type: $type")
    }
}

@Service
class ActionService(
    private val actionRegistry: ActionRegistry
) {
    suspend fun <T : ActionContext, R : ActionResult> execute(
        action: Action<T, R>,
        context: T
    ): R {
        return action.execute(context)
    }
}
```

#### 5. Исключения:
```kotlin
sealed class ActionException(message: String, cause: Throwable? = null) : Exception(message, cause)

class BuildException(message: String, cause: Throwable? = null) : ActionException(message, cause)
class AnalyzeException(message: String, cause: Throwable? = null) : ActionException(message, cause)
class TestExecuteException(message: String, cause: Throwable? = null) : ActionException(message, cause)
```

### Проверка соответствия требованиям:

✅ **Функциональные требования:**
- BuildAction поддерживает Designer и ibcmd
- ChangeAnalysisAction анализирует файловую систему
- RunTestAction запускает тесты с фильтрацией
- Составные назначения SourceSetPurpose
- Интеграция с существующими сервисами

✅ **Качественные требования:**
- Модульная структура обеспечивает тестируемость
- Легко добавлять новые модули
- Асинхронное выполнение через coroutines
- Специфичные исключения для каждого типа
- Подробное логирование через ActionContext

✅ **Технические ограничения:**
- Совместимость с Kotlin 2.1.20 и Spring Boot 3.5.3
- Интеграция с PlatformUtilityDsl
- Не требует обратной совместимости

## 🎨🎨🎨 EXITING CREATIVE PHASE

### Результаты творческого проектирования:

1. **Выбрана гибридная модульная архитектура** - оптимальный баланс между простотой и расширяемостью
2. **Определена структура файлов** - четкое разделение по модулям и доменам
3. **Спроектированы ключевые интерфейсы** - Action<T, R> с контекстом и результатами
4. **Создано руководство по реализации** - пошаговый план внедрения
5. **Проверено соответствие требованиям** - все функциональные и качественные требования покрыты

### Следующие шаги:
1. Реализация базовых интерфейсов и контекстов
2. Создание модулей build, change, test
3. Реализация ActionRegistry и ActionService
4. Интеграция с существующими сервисами
5. Написание тестов и документации 