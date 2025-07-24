package io.github.alkoleft.mcp.interfaces.websocket

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.alkoleft.mcp.application.services.TestLauncherService
import io.github.alkoleft.mcp.core.modules.GenericTestCase
import io.github.alkoleft.mcp.core.modules.GenericTestSuite
import io.github.alkoleft.mcp.core.modules.RunAllTestsRequest
import io.github.alkoleft.mcp.core.modules.RunListTestsRequest
import io.github.alkoleft.mcp.core.modules.RunModuleTestsRequest
import io.github.alkoleft.mcp.core.modules.TestExecutionResult
import io.github.alkoleft.mcp.core.modules.TestSummary
import kotlinx.coroutines.reactor.mono
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * WebSocket handler for real-time YAXUnit test execution.
 * Provides reactive streaming of test results for dynamic test execution.
 */
private val logger = KotlinLogging.logger {  }

@Component
class YaXUnitWebSocketHandler(
    private val testLauncher: TestLauncherService,
) : WebSocketHandler {
    private val objectMapper = jacksonObjectMapper()

    override fun handle(session: WebSocketSession): Mono<Void> {
        logger.info("WebSocket connection established: ${session.id}")

        return session
            .receive()
            .map { message -> message.payloadAsText }
            .doOnNext { payload -> logger.debug("Received WebSocket message: $payload") }
            .flatMap { payload -> processTestRequest(session, payload) }
            .doOnError { error -> logger.error("WebSocket error for session ${session.id}", error) }
            .doFinally { logger.info("WebSocket connection closed: ${session.id}") }
            .then()
    }

    private fun processTestRequest(
        session: WebSocketSession,
        payload: String,
    ): Flux<WebSocketMessage> =
        mono {
            try {
                val request = objectMapper.readValue(payload, WebSocketTestRequest::class.java)
                logger.info("Processing WebSocket test request for session ${session.id}")

                // Send acknowledgment
                WebSocketTestResponse(
                    type = "acknowledgment",
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    message = "Test request received, starting execution...",
                    sessionId = session.id,
                )

                // Execute tests and stream results
                executeTestsWithStreaming(session, request)
            } catch (e: Exception) {
                logger.error("Failed to process WebSocket test request", e)

                val errorMessage =
                    WebSocketTestResponse(
                        type = "error",
                        timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        message = "Failed to process test request: ${e.message}",
                        sessionId = session.id,
                        error = e.message,
                    )

                listOf(session.textMessage(objectMapper.writeValueAsString(errorMessage)))
            }
        }.flatMapMany { messages -> Flux.fromIterable(messages) }

    private suspend fun executeTestsWithStreaming(
        session: WebSocketSession,
        request: WebSocketTestRequest,
    ): List<WebSocketMessage> {
        val messages = mutableListOf<WebSocketMessage>()

        try {
            // Send start message
            val startMessage =
                WebSocketTestResponse(
                    type = "start",
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    message = "Starting test execution...",
                    sessionId = session.id,
                )
            messages.add(session.textMessage(objectMapper.writeValueAsString(startMessage)))

            // Create test execution request based on WebSocket request
            val testRequest =
                when (request.executionType) {
                    "all" ->
                        RunAllTestsRequest(
                            projectPath = Paths.get(request.projectPath),
                            testsPath = Paths.get(request.testsPath ?: "${request.projectPath}/tests"),
                            ibConnection = request.ibConnection,
                            platformVersion = request.platformVersion,
                        )

                    "module" ->
                        RunModuleTestsRequest(
                            projectPath = Paths.get(request.projectPath),
                            testsPath = Paths.get(request.testsPath ?: "${request.projectPath}/tests"),
                            ibConnection = request.ibConnection,
                            platformVersion = request.platformVersion,
                            moduleName =
                                request.moduleName
                                    ?: throw IllegalArgumentException("Module name required for module execution"),
                        )

                    "list" ->
                        RunListTestsRequest(
                            projectPath = Paths.get(request.projectPath),
                            testsPath = Paths.get(request.testsPath ?: "${request.projectPath}/tests"),
                            ibConnection = request.ibConnection,
                            platformVersion = request.platformVersion,
                            testNames =
                                request.testNames
                                    ?: throw IllegalArgumentException("Test names required for list execution"),
                        )

                    else -> throw IllegalArgumentException("Unknown execution type: ${request.executionType}")
                }

            // Execute tests
            val result =
                when (testRequest) {
                    is RunAllTestsRequest -> testLauncher.runAll(testRequest)
                    is RunModuleTestsRequest -> testLauncher.runModule(testRequest)
                    is RunListTestsRequest -> testLauncher.runList(testRequest)
                }

            // Send progress message
            val progressMessage =
                WebSocketTestResponse(
                    type = "progress",
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    message = "Tests completed, processing results...",
                    sessionId = session.id,
                )
            messages.add(session.textMessage(objectMapper.writeValueAsString(progressMessage)))

            // Send results
            val resultMessage =
                WebSocketTestResponse(
                    type = "result",
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    message = "Test execution completed",
                    sessionId = session.id,
                    testResult = WebSocketTestResult.fromExecutionResult(result),
                )
            messages.add(session.textMessage(objectMapper.writeValueAsString(resultMessage)))

            // Send completion message
            val completionMessage =
                WebSocketTestResponse(
                    type = "complete",
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    message = "Test execution completed successfully",
                    sessionId = session.id,
                )
            messages.add(session.textMessage(objectMapper.writeValueAsString(completionMessage)))
        } catch (e: Exception) {
            logger.error("WebSocket test execution failed", e)

            val errorMessage =
                WebSocketTestResponse(
                    type = "error",
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    message = "Test execution failed: ${e.message}",
                    sessionId = session.id,
                    error = e.message,
                )
            messages.add(session.textMessage(objectMapper.writeValueAsString(errorMessage)))
        }

        return messages
    }
}

/**
 * WebSocket test request data structure
 */
data class WebSocketTestRequest(
    // "all", "module", "list"
    val executionType: String,
    val projectPath: String,
    val testsPath: String? = null,
    val ibConnection: String,
    val platformVersion: String? = null,
    val moduleName: String? = null,
    val testNames: List<String>? = null,
    // For future dynamic test execution
    val testModuleText: String? = null,
)

/**
 * WebSocket test response data structure
 */
data class WebSocketTestResponse(
    // "acknowledgment", "start", "progress", "result", "complete", "error"
    val type: String,
    val timestamp: String,
    val message: String,
    val sessionId: String,
    val testResult: WebSocketTestResult? = null,
    val error: String? = null,
)

/**
 * WebSocket-specific test result structure
 */
data class WebSocketTestResult(
    val success: Boolean,
    val duration: Long,
    val summary: WebSocketTestSummary,
    val testSuites: List<WebSocketTestSuite>,
    val error: String? = null,
) {
    companion object {
        fun fromExecutionResult(result: TestExecutionResult): WebSocketTestResult =
            WebSocketTestResult(
                success = result.success,
                duration = result.duration.toMillis(),
                summary = WebSocketTestSummary.fromGenericSummary(result.report.summary),
                testSuites = result.report.testSuites.map { WebSocketTestSuite.fromGenericSuite(it) },
                error = result.error?.message,
            )
    }
}

data class WebSocketTestSummary(
    val totalTests: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val successRate: Double,
) {
    companion object {
        fun fromGenericSummary(summary: TestSummary): WebSocketTestSummary =
            WebSocketTestSummary(
                totalTests = summary.totalTests,
                passed = summary.passed,
                failed = summary.failed,
                skipped = summary.skipped,
                successRate = summary.successRate,
            )
    }
}

data class WebSocketTestSuite(
    val name: String,
    val tests: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val duration: Long,
    val testCases: List<WebSocketTestCase>,
) {
    companion object {
        fun fromGenericSuite(suite: GenericTestSuite): WebSocketTestSuite =
            WebSocketTestSuite(
                name = suite.name,
                tests = suite.tests,
                passed = suite.passed,
                failed = suite.failed,
                skipped = suite.skipped,
                duration = suite.duration.toMillis(),
                testCases = suite.testCases.map { WebSocketTestCase.fromGenericTestCase(it) },
            )
    }
}

data class WebSocketTestCase(
    val name: String,
    val status: String,
    val duration: Long,
    val errorMessage: String? = null,
) {
    companion object {
        fun fromGenericTestCase(testCase: GenericTestCase): WebSocketTestCase =
            WebSocketTestCase(
                name = testCase.name,
                status = testCase.status.name,
                duration = testCase.duration.toMillis(),
                errorMessage = testCase.errorMessage,
            )
    }
}
