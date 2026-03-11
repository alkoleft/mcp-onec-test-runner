# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - 2025-03-12

### Added
- **Configuration Dump Support** - Add support for configuration dump in full/incremental/partial modes
- **Partial Configuration Loading** - Add partial configuration loading support
- **Compact Test Response Format** - Add compact test response format for reduced payload
- **Enterprise Launch Keys** - Add additional launch keys for Enterprise edition
- **1C Logs in Error Response** - Return 1C logs in MCP error response
- **Multi-source-set Hash Storage** - Add support for hash storage across multiple source sets

### Changed
- **Log Cleanup** - Add log cleanup before tool execution
- **DTO Simplification** - Simplify DTOs and add null safety

### Fixed
- **EDT Dump Path** - Fix EDT dump writing to basePath + source-set
- **Dump Extension Path** - Fix dump extension path ignoring case
- **Dump Flow Simplification** - Simplify dump flow and reduce test response payload

### Maintenance
- Apply ktlint formatting
- Update README

## [0.4.0] - 2025-02-15

### Added
- Integration with 1C:EDT (Enterprise Development Tools)
- EDT CLI support with interactive mode
- Auto-start option for EDT CLI
- Additional launch keys configuration

### Changed
- Improved incremental build with content hash verification
- Two-phase change detection (timestamp + hash)

### Fixed
- Various EDT-related path resolution issues
- Build state management for large projects

## [0.3.1] - 2025-01-20

### Fixed
- Bug fixes and stability improvements

## [0.3.0] - 2025-01-10

### Added
- IBCMD support for headless builds
- Syntax checking via CheckConfig and CheckModules

## [0.2.0] - 2024-12-01

### Added
- Incremental build support
- Change detection system
- Multiple source set support

## [0.1.0] - 2024-11-01

### Added
- Initial release
- YaXUnit test execution
- Project building via DESIGNER
- Basic MCP tools