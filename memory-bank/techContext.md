# Технический контекст MCP YaxUnit Runner

## Стек технологий и версии
- Kotlin 2.1.20 (JDK 17)
- Spring Boot 3.5.3
- Spring AI MCP Server 1.0.0
- Reactor Core 3.6.11
- JUnit Jupiter 5.11.4

## Архитектура
### Принципы
- SOLID
- Слоистая архитектура:
  - Presentation (API/UI)
  - Application (Use Cases)
  - Domain (Business Logic)
  - Infrastructure (External Services)

### Kotlin практики
- Data Classes для DTO
- Extension Functions для расширения функциональности
- Coroutines для асинхронных операций
- When expressions для pattern matching
- Null safety и строгая типизация
- Sealed Classes для type-safe иерархий
- Type aliases для сложных типов

## Зависимости
### Spring экосистема
- Spring Boot (Web, Cache)
- Spring AI MCP Server + WebFlux
- Spring WebFlux

### Инфраструктура
- Jackson 2.15.2 (Core, Databind, XML, Kotlin)
- Logback 1.5.18 + SLF4J
- Kotlin Logging 7.0.3
- Kotlinx CLI 0.3.6

### Логирование
- **KotlinLogging 7.0.3** - библиотека для удобного логирования в Kotlin
- Примеры использования:
  ```kotlin
  // Создание логгера
  private val logger = KotlinLogging.logger {}
  
  // Различные уровни логирования
  logger.trace { "Трассировка выполнения" }
  logger.debug { "Отладочная информация: $variable" }
  logger.info { "Информационное сообщение" }
  logger.warn { "Предупреждение: $warningMessage" }
  logger.error { "Ошибка: $errorMessage" }
  
  // Логирование с параметрами
  logger.info { "Запрос обработан: userId=$userId, duration=${duration}ms" }
  
  // Логирование исключений
  logger.error(exception) { "Произошла ошибка при обработке запроса" }
  ```

## Качество
### Тестирование (80% покрытия)
- **Kotest** - основной фреймворк для тестирования
- **Should Spec** - стиль написания тестов с использованием `should` синтаксиса
- Примеры использования:
  ```kotlin
  class MyServiceTest : ShouldSpec({
    should("возвращать корректный результат") {
      val service = MyService()
      val result = service.process("input")
      result shouldBe "expected_output"
    }
    
    context("когда входные данные некорректны") {
      should("выбрасывать исключение") {
        val service = MyService()
        shouldThrow<IllegalArgumentException> {
          service.process("")
        }
      }
    }
    
    should("обрабатывать пустой список") {
      val service = MyService()
      val result = service.processList(emptyList())
      result shouldBe emptyList<String>()
    }
  })
  ```
- Unit: бизнес-логика
- Integration: взаимодействие компонентов
- E2E: критические сценарии
- JaCoCo для анализа покрытия

### Документация
- KDoc для публичного API
- Пользовательская (установка, использование)
- Техническая (ADR, диаграммы, деплой)

## Сборка и развертывание
- Gradle + Kotlin DSL
- GitHub Packages для артефактов
- ktlint для стиля кода
- Переменные окружения:
  - GITHUB_ACTOR
  - GITHUB_TOKEN 