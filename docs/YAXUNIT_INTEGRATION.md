# YaXUnit Integration

This document describes the YaXUnit test execution functionality implemented in the MCP YAxUnit Runner.

## Overview

The YaXUnit integration provides a complete test execution framework for 1C:Enterprise applications using the YaXUnit testing framework. It supports running tests through 1C:Enterprise with proper configuration management and report parsing.

## Features

### Test Execution
- **Enterprise Mode**: Run tests using 1C:Enterprise with the `RunUnitTests` parameter
- **Configuration Management**: Generate JSON configuration files for test execution
- **Report Parsing**: Parse test reports in multiple formats (jUnit XML, JSON, YaXUnit JSON, Plain Text)
- **Filtering**: Support for running all tests, module-specific tests, or specific test lists

### Platform Integration
- **Enterprise DSL**: Fluent API for working with 1C:Enterprise
- **Utility Detection**: Automatic detection of 1C platform utilities
- **Cross-platform Support**: Works on Windows, Linux, and macOS

## Architecture

### Core Components

1. **YaXUnitTestAction**: Main action for test execution
2. **ProcessYaXUnitRunner**: Executes tests via 1C:Enterprise process
3. **JsonYaXUnitConfigWriter**: Generates YaXUnit configuration files
4. **EnhancedReportParser**: Parses test reports in multiple formats
5. **Enterprise DSL**: Fluent API for 1C:Enterprise operations

### Test Execution Flow

```
YaXUnitTestAction
    ↓
Create Test Request
    ↓
Generate Configuration (JsonYaXUnitConfigWriter)
    ↓
Execute Tests (ProcessYaXUnitRunner)
    ↓
Parse Report (EnhancedReportParser)
    ↓
Return TestExecutionResult
```

## Usage

### Basic Test Execution

```kotlin
val testAction = YaXUnitTestAction(
    platformUtilityDsl,
    utilLocator,
    configWriter,
    reportParser
)

val result = testAction.run(filter = null, properties = applicationProperties)
```

### Enterprise DSL Usage

```kotlin
val result = platformUtilityDsl.enterprise("8.3.18") {
    connectToFile("/path/to/database")
    user("Admin")
    disableStartupDialogs()
    disableStartupMessages()
    
    runTests(configPath)
}
```

### Configuration Generation

```kotlin
val configWriter = JsonYaXUnitConfigWriter()
val request = RunAllTestsRequest(
    projectPath = Path.of("/project"),
    testsPath = Path.of("/project/tests"),
    ibConnection = "File=\"/path/to/db\";",
    platformVersion = "8.3.18"
)

val configPath = configWriter.createTempConfig(request)
```

## Configuration

### YaXUnit Configuration Format

The system generates JSON configuration files compatible with YaXUnit:

```json
{
    "filter": {
        "modules": ["Module1", "Module2"]
    },
    "reportFormat": "jUnit",
    "reportPath": "/path/to/report.xml",
    "closeAfterTests": true,
    "showReport": false,
    "logging": {
        "file": "/path/to/logs/tests.log",
        "console": false,
        "level": "info"
    }
}
```

### Supported Report Formats

1. **jUnit XML**: Standard jUnit XML format
2. **JSON**: Generic JSON test reports
3. **YaXUnit JSON**: YaXUnit-specific JSON format
4. **Plain Text**: Simple text-based reports

## Platform Support

### Utility Types

- `ENTERPRISE`: 1C:Enterprise thin client (`1cv8c.exe`)
- `DESIGNER`: 1C:Designer (`1cv8.exe`)
- `INFOBASE_MANAGER_IBCMD`: ibcmd utility
- `IBSRV`: 1C:Enterprise server

### Platform Detection

The system automatically detects the platform and adjusts command parameters accordingly:

- **Windows**: Uses `/` prefix for parameters
- **Linux/macOS**: Uses `-` prefix for parameters

## Error Handling

### Common Error Scenarios

1. **Utility Not Found**: System will attempt to locate 1C utilities in standard locations
2. **Connection Failed**: Proper error messages for database connection issues
3. **Test Execution Failed**: Detailed error reporting with exit codes
4. **Report Parsing Failed**: Graceful fallback with warning messages

### Error Recovery

- Automatic retry for transient failures
- Detailed logging for debugging
- Graceful degradation when optional features fail

## Testing

### Unit Tests

The implementation includes comprehensive unit tests:

- Configuration generation tests
- Report parsing tests
- Format detection tests
- Error handling tests

### Integration Tests

- End-to-end test execution
- Platform utility detection
- Report generation and parsing

## Dependencies

### Required Dependencies

- Jackson (JSON processing)
- Kotlin Coroutines (async operations)
- Spring Boot (dependency injection)
- JUnit 5 (testing)

### Optional Dependencies

- Logging frameworks (SLF4J, Logback)
- Mock frameworks (Mockito)

## Configuration Properties

### Application Properties

```yaml
app:
  base-path: /path/to/project
  platform-version: "8.3.18"
  connection:
    connection-string: "File=/path/to/database;"
    user: "Admin"
    password: null
  source-set:
    items:
      - name: "configuration"
        path: "src"
        type: "CONFIGURATION"
        purpose: ["MAIN"]
      - name: "tests"
        path: "tests"
        type: "CONFIGURATION"
        purpose: ["TESTS"]
```

## Best Practices

### Test Organization

1. **Module-based Structure**: Organize tests by modules
2. **Clear Naming**: Use descriptive test names
3. **Configuration Management**: Store test configurations separately
4. **Logging**: Enable appropriate logging levels

### Performance Optimization

1. **Parallel Execution**: Use parallel test execution when possible
2. **Incremental Testing**: Run only changed tests during development
3. **Resource Management**: Proper cleanup of temporary files
4. **Caching**: Cache utility locations and configurations

### Security Considerations

1. **Credential Management**: Secure storage of database credentials
2. **File Permissions**: Proper file permissions for test artifacts
3. **Network Security**: Secure database connections
4. **Input Validation**: Validate all configuration inputs

## Troubleshooting

### Common Issues

1. **1C Platform Not Found**
   - Check platform installation
   - Verify PATH environment variable
   - Use explicit platform version

2. **Database Connection Failed**
   - Verify connection string format
   - Check database accessibility
   - Validate user credentials

3. **Test Execution Timeout**
   - Increase timeout settings
   - Check system resources
   - Review test complexity

4. **Report Parsing Errors**
   - Verify report format
   - Check file permissions
   - Review report content

### Debug Mode

Enable debug logging for detailed troubleshooting:

```yaml
logging:
  level:
    io.github.alkoleft.mcp: DEBUG
```

## Future Enhancements

### Planned Features

1. **Test Discovery**: Automatic test discovery in modules
2. **Parallel Execution**: Native support for parallel test execution
3. **Test Categories**: Support for test categories and tags
4. **Performance Metrics**: Detailed performance reporting
5. **Integration Testing**: Enhanced integration test support

### API Extensions

1. **Custom Report Formats**: Plugin system for custom report formats
2. **Test Frameworks**: Support for additional test frameworks
3. **CI/CD Integration**: Enhanced CI/CD pipeline integration
4. **Monitoring**: Real-time test execution monitoring

## Contributing

### Development Guidelines

1. **Code Style**: Follow Kotlin coding conventions
2. **Testing**: Maintain high test coverage
3. **Documentation**: Update documentation for new features
4. **Backward Compatibility**: Maintain API compatibility

### Testing Guidelines

1. **Unit Tests**: Write unit tests for all new functionality
2. **Integration Tests**: Include integration tests for complex features
3. **Performance Tests**: Test performance impact of changes
4. **Platform Tests**: Test on multiple platforms

## License

This implementation is part of the MCP YAxUnit Runner project and follows the project's licensing terms. 