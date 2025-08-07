package io.github.alkoleft.mcp.core.modules.strategy

@Deprecated("Command builders strategy is replaced by EnterpriseDsl. This file remains for API compatibility and will be removed later.")
interface CommandBuilderStrategy

@Deprecated("ConnectionType is no longer used; EnterpriseDsl handles connection strings directly.")
enum class ConnectionType { FILE_DATABASE, SERVER_DATABASE, WEB_SERVER, UNKNOWN }
