package io.github.alkoleft.mcp.infrastructure.yaxunit

import java.nio.file.Files
import java.nio.file.Path

fun configPath(): Path = Files.createTempFile("yaxunit-config-", ".json")

fun reportPath() = Files.createTempFile("yaxunit-report-", ".xml").toString()

fun logPath() = Files.createTempFile("yaxunit-log-", ".log").toString()
