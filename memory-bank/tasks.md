## Task: Implement EDT CLI DSL (execute-on-call) and integrate utility discovery ✅ COMPLETED

- **Overview of changes**:
  - ✅ Added new DSL `edt` to execute 1C:EDT CLI commands immediately and return results.
  - ✅ Added new utility type `EDT_CLI` and extended locator search paths (Linux) to support `/opt/1C/1CE/components/<version>/1cedtcli`.
  - ✅ Provided comprehensive command surface based on official EDT CLI documentation.
  - ✅ Wired DSL entry point via `PlatformDsl.edt(...)`.

- **Files modified/added**:
  - ✅ `src/main/kotlin/io/github/alkoleft/mcp/core/modules/PlatformDomain.kt` - `EDT_CLI` utility type already existed
  - ✅ `src/main/kotlin/io/github/alkoleft/mcp/infrastructure/platform/search/SearchStrategy.kt` - EDT base path already existed
  - ✅ `src/main/kotlin/io/github/alkoleft/mcp/infrastructure/platform/dsl/PlatformDsl.kt` - `edt` entry point already existed
  - ✅ `src/main/kotlin/io/github/alkoleft/mcp/infrastructure/platform/dsl/edt/EdtDsl.kt` - Complete implementation with `EdtDsl`, `EdtContext`, `EdtResult`
  - ✅ `src/test/kotlin/io/github/alkoleft/mcp/infrastructure/platform/dsl/edt/EdtDslTest.kt` - Comprehensive test coverage
  - ✅ `README.md` - Added detailed documentation and usage examples

- **Implementation completed**:
  1. ✅ `EDT_CLI("1cedtcli")` utility type was already present in `UtilityType`
  2. ✅ Linux search with `VersionLocation("/opt/1C/1CE/components")` was already configured
  3. ✅ Created `EdtDsl` with immediate execution using `ProcessExecutor`; implemented comprehensive EDT CLI commands
  4. ✅ DSL exposed via `PlatformDsl.edt(version) { ... }` using `PlatformUtilityContext`
  5. ✅ Verified build success and added comprehensive usage examples in README

- **Key architectural decisions**:
  - ✅ Removed inheritance from `BasePlatformDsl` and `BasePlatformContext` as EDT CLI has different command structure
  - ✅ Implemented standalone `EdtDsl` and `EdtContext` classes specific to EDT CLI
  - ✅ Added comprehensive command set based on official EDT CLI documentation
  - ✅ Maintained clean separation of concerns between EDT CLI and other platform utilities

- **EDT CLI Commands Implemented (Complete List)**:
  - ✅ **Основные команды**: `version()`, `help()`, `run(vararg args: String)`
  - ✅ **Build commands**: `build(projects: List<String>? = null, yes: Boolean = true)`, `buildProjects(vararg projectNames: String, yes: Boolean = true)`
  - ✅ **Directory commands**: `cd(directory: String? = null)`
  - ✅ **Clean-up commands**: `cleanUpSource(projectPath: String? = null, projectName: String? = null, includeFullSupportObjects: Boolean = false)`
  - ✅ **Delete commands**: `delete(projects: List<String>? = null, yes: Boolean = true)`, `deleteProjects(vararg projectNames: String, yes: Boolean = true)`
  - ✅ **Export commands**: `export(projectPath: String? = null, projectName: String? = null, configurationFiles: String)`
  - ✅ **Format commands**: `formatModules(projectPath: String? = null, projectName: String? = null)`
  - ✅ **Import commands**: `importProject(projectPath: String)`, `importConfiguration(configurationFiles: String, projectPath: String? = null, projectName: String? = null, version: String? = null, baseProjectName: String? = null, build: Boolean = false)`
  - ✅ **Infobase commands**: `infobase(details: Boolean = false, infobases: List<String>? = null)`, `infobaseCreate(name: String, version: String? = null, path: String? = null, configurationFile: String? = null)`, `infobaseDelete(names: List<String>? = null, name: String? = null, yes: Boolean = false, deleteContent: Boolean = false)`, `infobaseImport(name: String, project: String, build: Boolean = false)`
  - ✅ **Platform support commands**: `installPlatformSupport(version: String)`, `uninstallPlatformSupport(version: String)`, `platformVersions()`
  - ✅ **Project commands**: `project(details: Boolean = false, projects: List<String>? = null)`
  - ✅ **Script commands**: `script()`, `scriptInfo(scriptName: String, content: Boolean = false)`, `scriptLoad(scriptPath: String, recursive: Boolean = true, namespace: String? = null)`
  - ✅ **Sort project commands**: `sortProject(projectPaths: List<String>? = null, projectNames: List<String>? = null)`
  - ✅ **Validate commands**: `validate(outputFile: String, projectPaths: List<String>? = null, projectNames: List<String>? = null)`

- **Key fixes applied**:
  - Fixed suspend function issue in `EdtContext.buildEdtArgs()` by making it non-suspend and using `locateUtilitySync()`
  - Added comprehensive test coverage for all EDT DSL components
  - Added detailed documentation with usage examples
  - Implemented proper command structure based on official EDT CLI documentation
  - Fixed argument parsing and command structure to match official EDT CLI syntax

- **Testing completed**:
  - ✅ All tests pass successfully
  - ✅ EDT DSL functionality verified through unit tests
  - ✅ Build compilation successful
  - ✅ Integration with existing platform infrastructure verified

- **Documentation added**:
  - ✅ Comprehensive README section with usage examples
  - ✅ Command reference and platform support information
  - ✅ Integration examples for developers
  - ✅ Link to official EDT CLI documentation

- **Official Documentation Reference**:
  - ✅ All commands implemented based on [1C:Enterprise Development Tools Documentation](https://its.1c.ru/db/edtdoc#content:10608:hdoc)
  - ✅ Commands cover all major EDT CLI functionality: build, import/export, infobase management, project management, validation, formatting, and more
  - ✅ Full command set matches official documentation exactly
