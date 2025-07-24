# 🎨🎨🎨 ENTERING CREATIVE PHASE: ARCHITECTURE DESIGN

## Component: MCP YAXUnit Runner - Overall System Architecture

### Component Description
A Model Context Protocol (MCP) server for running modular tests on 1C:Enterprise solutions using the YAXUnit framework. The system needs to support cross-platform execution (Linux/Windows), incremental builds, file watching, WebSocket real-time testing, and multiple transport protocols (stdio/SSE).

### Requirements & Constraints

#### Functional Requirements
- Execute YAXUnit tests (all/module/specific list)
- Support both full and incremental builds
- Cross-platform compatibility (Linux/Windows)
- Real-time test execution via WebSocket
- MCP protocol integration with Spring AI
- CLI interface with multiple subcommands
- File monitoring for change detection
- Platform-specific utility discovery (1cv8c/ibcmd)

#### Non-Functional Requirements
- High performance with minimal resource usage
- Maintainable and testable code (80% coverage)
- SOLID principles compliance
- Thread-safe operations for concurrent test execution
- Robust error handling and recovery
- Extensible architecture for future enhancements

#### Technical Constraints
- Kotlin 2.1.20 with JDK 17
- Spring Boot 3.5.3 + Spring AI MCP Server
- No direct database dependency (file-based storage)
- Must integrate with existing 1C:Enterprise toolchain

### Architecture Options Analysis

#### Option 1: Monolithic Layered Architecture
```kotlin
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│  ├─ MCP Commands                        │
│  ├─ CLI Interface (picocli)             │
│  └─ WebSocket Endpoints                 │
├─────────────────────────────────────────┤
│           Application Layer             │
│  ├─ TestExecutionService                │
│  ├─ BuildOrchestrationService           │
│  └─ FileMonitoringService               │
├─────────────────────────────────────────┤
│             Domain Layer                │
│  ├─ Test Entities & Value Objects       │
│  ├─ Build State Management              │
│  └─ Business Rules & Validation         │
├─────────────────────────────────────────┤
│          Infrastructure Layer           │
│  ├─ File System Access                  │
│  ├─ Process Execution                   │
│  ├─ Platform Adapters                   │
│  └─ External Tool Integration           │
└─────────────────────────────────────────┘
```

**Pros:**
- Clear separation of concerns
- Easy to understand and maintain
- Follows established patterns
- Good testability with layer isolation
- Straightforward dependency management

**Cons:**
- Potential performance overhead from layer crossing
- Risk of anemic domain model
- Less flexibility for future distributed scenarios
- Possible tight coupling between layers

#### Option 2: Modular Hexagonal Architecture
```kotlin
                    ┌─────────────────────┐
                    │   Application Core  │
                    │                     │
    ┌───────────────│  ┌─ Test Domain ─┐  │───────────────┐
    │               │  ├─ Build Domain─┤  │               │
    │               │  └─ Report Domain┘  │               │
    │               └─────────────────────┘               │
    │                                                     │
┌───▼────┐    ┌──────────┐         ┌──────────┐    ┌─────▼───┐
│  MCP   │    │   CLI    │         │WebSocket │    │ File    │
│Adapter │    │ Adapter  │         │ Adapter  │    │ System  │
└────────┘    └──────────┘         └──────────┘    │ Adapter │
                                                    └─────────┘
                  ┌──────────┐         ┌──────────┐
                  │ Process  │         │ Platform │
                  │ Adapter  │         │ Adapter  │
                  └──────────┘         └──────────┘
```

**Pros:**
- High testability with adapter mocking
- Clear separation of business logic
- Easy to add new interfaces/protocols
- Domain-centric design
- Better flexibility and extensibility

**Cons:**
- Higher initial complexity
- More interfaces and abstraction layers
- Potential over-engineering for current scope
- Learning curve for team members

#### Option 3: Event-Driven Microkernel Architecture
```kotlin
          ┌─────────────────────────────────┐
          │         Event Bus Core          │
          │  ┌─────────────────────────────┐ │
          │  │     Microkernel Core        │ │
          │  │  ├─ Plugin Registry         │ │
          │  │  ├─ Event Dispatcher        │ │
          │  │  └─ Service Locator         │ │
          │  └─────────────────────────────┘ │
          └─────────────────────────────────┘
                           │
    ┌──────────────────────┼──────────────────────┐
    │                      │                      │
┌───▼────┐         ┌───────▼────┐         ┌──────▼──┐
│  Test  │         │   Build    │         │ Report  │
│ Plugin │         │   Plugin   │         │ Plugin  │
└────────┘         └────────────┘         └─────────┘
    │                      │                      │
┌───▼────┐         ┌───────▼────┐         ┌──────▼──┐
│  MCP   │         │File Monitor│         │Platform │
│ Plugin │         │   Plugin   │         │ Plugin  │
└────────┘         └────────────┘         └─────────┘
```

**Pros:**
- Highly extensible and pluggable
- Event-driven reactive design
- Easy to add new functionality
- Good for complex workflow management
- Excellent separation of concerns

**Cons:**
- High complexity for current requirements
- Event debugging can be challenging
- Potential performance overhead
- Over-engineered for MVP scope

### Recommended Approach: Enhanced Modular Layered Architecture

I recommend a hybrid approach that combines the simplicity of layered architecture with the modularity benefits of hexagonal design:

```kotlin
┌─────────────────────────────────────────────────────────┐
│                    Interface Layer                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │
│  │ MCP Server  │ │ CLI Handler │ │ WebSocket Server    │ │
│  │ (Spring AI) │ │ (PicoCLI)   │ │ (Spring WebFlux)    │ │
│  └─────────────┘ └─────────────┘ └─────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────┐
│                 Application Services                    │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────┐ │
│  │ TestLauncher    │ │ BuildService    │ │ FileWatcher │ │
│  │ (Facade)        │ │ (Orchestrator)  │ │ Service     │ │
│  └─────────────────┘ └─────────────────┘ └─────────────┘ │
└─────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────┐
│                    Core Modules                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │
│  │ Build State │ │ Report      │ │ Configuration       │ │
│  │ Manager     │ │ Parser      │ │ Manager             │ │
│  └─────────────┘ └─────────────┘ └─────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────┐
│                Infrastructure Layer                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │
│  │ UtilLocator │ │ HashStorage │ │ YaXUnitRunner       │ │
│  │ (Platform)  │ │ (MapDB)     │ │ (Process)           │ │
│  └─────────────┘ └─────────────┘ └─────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

**Key Architectural Decisions:**

1. **Interface Layer**: Clean separation of different interaction methods (MCP/CLI/WebSocket)
2. **Application Services**: Business orchestration without tight coupling to infrastructure
3. **Core Modules**: Domain-specific logic with clear responsibilities
4. **Infrastructure Layer**: Platform-specific implementations and external integrations

### Implementation Guidelines

#### 1. Module Structure
```kotlin
// Define clear module boundaries
interface TestLauncher {
    suspend fun runAll(request: TestExecutionRequest): TestExecutionResult
    suspend fun runModule(request: ModuleTestRequest): TestExecutionResult 
    suspend fun runList(request: ListTestRequest): TestExecutionResult
}

// Application service implementation
class TestLauncherImpl(
    private val buildService: BuildService,
    private val utilLocator: UtilLocator,
    private val yaXUnitRunner: YaXUnitRunner,
    private val reportParser: ReportParser
) : TestLauncher {
    // Implementation with dependency injection
}
```

#### 2. Dependency Management
```kotlin
// Use Spring's dependency injection with interfaces
@Configuration
class ApplicationConfig {
    @Bean
    fun testLauncher(
        buildService: BuildService,
        utilLocator: UtilLocator,
        yaXUnitRunner: YaXUnitRunner,
        reportParser: ReportParser
    ): TestLauncher = TestLauncherImpl(buildService, utilLocator, yaXUnitRunner, reportParser)
}
```

#### 3. Error Handling Strategy
```kotlin
// Centralized error handling with sealed classes
sealed class YaXUnitError : Exception() {
    data class UtilNotFound(val utility: String) : YaXUnitError()
    data class BuildFailed(val reason: String) : YaXUnitError()
    data class TestExecutionFailed(val details: String) : YaXUnitError()
}
```

#### 4. Configuration Management
```kotlin
// Type-safe configuration with validation
@ConfigurationProperties(prefix = "yaxunit")
data class YaXUnitConfiguration(
    val projectPath: String,
    val testsPath: String = "./tests",
    val ibConnection: String,
    val platformVersion: String? = null,
    val logFile: String? = null
)
```

#### 5. Spring WebFlux WebSocket Implementation
```kotlin
// WebSocket configuration using Spring WebFlux
@Configuration
@EnableWebFlux
class WebSocketConfig {
    
    @Bean
    fun webSocketHandlerMapping(yaxUnitWebSocketHandler: YaxUnitWebSocketHandler): HandlerMapping {
        return SimpleUrlHandlerMapping().apply {
            order = 1
            urlMap = mapOf("/yaxunit" to yaxUnitWebSocketHandler)
        }
    }
    
    @Bean
    fun webSocketHandlerAdapter() = WebSocketHandlerAdapter()
}

// WebSocket handler for real-time test execution
@Component
class YaxUnitWebSocketHandler(
    private val testLauncher: TestLauncher
) : WebSocketHandler {
    
    override fun handle(session: WebSocketSession): Mono<Void> {
        val input = session.receive()
            .map { it.payloadAsText }
            .map { parseTestRequest(it) }
            .doOnNext { request -> 
                logger.info("Received test request: ${request.testModuleName}")
            }
            .flatMap { request ->
                // Execute test asynchronously and stream results
                executeTestAndStreamResults(request, session)
            }
            .doOnError { error ->
                logger.error("WebSocket test execution error", error)
            }
            .onErrorResume { error ->
                session.send(Mono.just(session.textMessage(
                    createErrorResponse(error).toJson()
                ))).then()
            }
            .then()
            
        return input
    }
    
    private fun executeTestAndStreamResults(
        request: TestRequest, 
        session: WebSocketSession
    ): Mono<Void> {
        return Mono.fromCallable {
            // Create temporary test module file
            val tempFile = createTempTestFile(request.testModuleText)
            val testExecutionRequest = TestExecutionRequest(
                projectPath = request.projectPath,
                tempTestFile = tempFile
            )
            
            // Execute test
            runBlocking { testLauncher.runModule(testExecutionRequest) }
        }
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap { result ->
            // Send test results back through WebSocket
            val response = TestExecutionResponse(
                success = result.success,
                report = result.report,
                duration = result.duration
            )
            session.send(Mono.just(session.textMessage(response.toJson())))
        }
        .doFinally {
            // Cleanup temporary files
            cleanupTempFiles()
        }
    }
}
```

### Verification Checkpoint

**Architecture Verification:**
✅ **Clear separation of concerns**: Each layer has distinct responsibilities
✅ **Testability**: Interfaces enable easy mocking and unit testing  
✅ **Extensibility**: New interfaces/protocols can be added without core changes
✅ **Cross-platform support**: Platform-specific logic isolated in infrastructure layer
✅ **Performance considerations**: Minimal layer crossing, efficient service orchestration
✅ **SOLID compliance**: Single responsibility, dependency inversion, interface segregation
✅ **Spring Boot integration**: Natural fit with Spring's architecture patterns

The architecture satisfies all functional and non-functional requirements while maintaining simplicity and extensibility for future enhancements.

# 🎨🎨🎨 EXITING CREATIVE PHASE 