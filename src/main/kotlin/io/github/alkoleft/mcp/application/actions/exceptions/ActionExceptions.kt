package io.github.alkoleft.mcp.application.actions.exceptions

/**
 * Base exception for all action-related errors
 */
sealed class ActionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown during build operations
 */
class BuildException(message: String, cause: Throwable? = null) : ActionException(message, cause)

/**
 * Exception thrown during change analysis operations
 */
class AnalyzeException(message: String, cause: Throwable? = null) : ActionException(message, cause)

/**
 * Exception thrown during test execution operations
 */
class TestExecuteException(message: String, cause: Throwable? = null) : ActionException(message, cause) 