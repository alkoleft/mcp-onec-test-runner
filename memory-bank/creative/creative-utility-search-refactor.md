# 🎨🎨🎨 ENTERING CREATIVE PHASE: Architecture + Algorithm

## Component Description
Refactor the cross-platform utility discovery for 1C Platform and EDT tools. The system must enumerate candidate locations across OSes, extract and compare versions (including masked prefixes and keyword `latest`), and select the best matching executable for utilities: `DESIGNER`, `THIN_CLIENT`, `THICK_CLIENT`, `IBCMD`, `IBSRV`, `EDT_CLI`.

Impacted components:
- `infrastructure/platform/search/SearchLocation.kt`
- `infrastructure/platform/search/SearchStrategy.kt`
- `infrastructure/platform/locator/UtilityLocator.kt`
- `infrastructure/platform/locator/UtilityValidator.kt` (behavior only)
- `infrastructure/platform/dsl/common/PlatformUtilityContext.kt` (behavioral: default latest for EDT)
- New: `infrastructure/platform/search/VersionResolver.kt` (parser + matcher)
- New: `infrastructure/platform/search/DirectoryEnumeratingLocation.kt` (configurable directory scanner)

## Requirements & Constraints
- Support version inputs: exact (e.g., 8.3.24.1234), masked prefix (e.g., 8.3.20), and keyword `latest`.
- Cross-platform: Linux, Windows, macOS.
- Discover `EDT_CLI` across OS-specific locations in both system components and user installations.
- Prefer the highest version among candidates matching the requirement.
- Preserve current public API and caching behavior in `UtilityLocator`.
- Performance: limit directory scans to known bases; stop early on exact match; cache successful resolutions.
- Robustness: ignore missing env vars and non-existing roots; treat `~` via home expansion.

## Options Analysis

### Architecture Options
1) Enhanced Strategy + Location (Recommended)
- Keep `SearchStrategy` orchestrating a list of `SearchLocation` providers.
- Add `DirectoryEnumeratingLocation` capable of scanning versioned directories and composing candidate executables.
- Introduce `VersionResolver` for parsing, matching, and selecting the best candidate.
- Pros: Minimal API change, composable, testable, aligns with current design.
- Cons: Requires careful factoring to avoid duplicate FS scans.

2) Centralize Scanning in `UtilityLocator`
- Strategies provide only base roots; `UtilityLocator` performs all enumeration and selection.
- Pros: Single place for logic; straightforward flow.
- Cons: Violates separation of concerns; makes `UtilityLocator` heavy and harder to test.

3) Config-Driven Discovery Layer
- External YAML/JSON describes roots and patterns; generic scanner loads config.
- Pros: Flexible; easy to extend without code changes.
- Cons: More moving parts; validation complexity; not required by current scope.

4) OS-Specific Plugins
- Separate plugins per OS with their own scanning logic.
- Pros: Clear separation; optimized per OS.
- Cons: Code duplication; higher maintenance; unnecessary for current needs.

Selected: Option 1.

### Algorithm Options (Version Resolution)
1) Universal Numeric Parser → Version value type → Lexicographic Compare (Recommended)
- Parse any version-like string by extracting numeric tokens in order.
- Represent versions as `Version(parts: List<Int>)` with `Comparable` and `startsWith(mask)`.
- Matching: exact equals; mask = prefix match on parts; `latest` = max compare.
- Pros: One parser for all inputs; simple, fast, deterministic.
- Cons: Callers must pre-extract the version substring from rich paths when needed.

2) Adapt a SemVer Library
- Pros: Off-the-shelf comparisons.
- Cons: Versions are not strict SemVer; EDT uses year-based scheme → requires adapters anyway.

3) Filesystem Timestamps for Latest
- Pros: No parsing needed.
- Cons: Unreliable across installs; not tied to version semantics.

Selected: Option 1.

## Recommended Approach
- Extend `SearchLocation` set with `DirectoryEnumeratingLocation` where the path layout is versioned.
- Gather all candidate executables with optional extracted version strings.
- Use `VersionResolver.selectBest(candidates, requirement)` to choose the path, supporting exact, mask, and `latest` behavior.
- Keep `PathEnvironmentLocation` as the final fallback.
- Cache the resolved path in `UtilityLocator` keyed by `(utility, versionInput)`.
- Default `versionInput = "latest"` for `EDT_CLI` in `PlatformUtilityContext`.

## Implementation Guidelines

### New: Version and VersionResolver
- Location: `infrastructure/platform/search/Version.kt`, `infrastructure/platform/search/VersionResolver.kt`
- Responsibilities:
  - `Version(parts: List<Int>)` implements `Comparable<Version>` and `startsWith(mask: Version)`.
  - `Version.parse(input: String): Version?` universal numeric parser (extracts digits in order), callers pre-extract version substring from paths when needed.
  - `VersionResolver.selectBest(candidates: List<Pair<Path, String?>>, requirement: String?): Path?`:
    - requirement null or `latest`: choose max by `Version` among candidates with versions, else first.
    - exact: versions equal to parsed requirement.
    - mask: prefix match using `startsWith()`.
- Comparison: lexicographic over up to 4 segments; missing segments treated as 0.
- Robustness: ignore non-numeric suffixes; prefer candidates with parsed versions; graceful fallback.

### New: DirectoryEnumeratingLocation
- Location: `infrastructure/platform/search/DirectoryEnumeratingLocation.kt`
- Constructor parameters:
  - `basePath: Path`
  - `relativeExecutableSubPath: String` (e.g., `bin`, `1cedt`)
  - `dirPatternRegex: Regex` (captures the version substring)
- Behavior:
  - Enumerate immediate subdirectories of `basePath` matching `dirPatternRegex`.
  - Extract version substring using the capturing group from `dirPatternRegex` and pass as the second of the pair.
  - Compose candidate path: `subdir / relativeExecutableSubPath / getExecutableName(utility)`.
  - Return `List<Pair<Path, String?>>` where the second is the captured version.

### SearchStrategy updates
- `search(utility: UtilityType, version: String?): UtilityLocation`
  - Build a flat list of candidates: `List<Pair<Path, String?>>` by:
    - If `location` is `VersionedLocation`, use `enumerateCandidates(utility)`.
    - Else use `generatePaths(utility, version)` and wrap as `(path, null)`.
  - Normalize `version` requirement: if null and `utility == EDT_CLI`, use `latest`.
  - Select a path with `VersionResolver.selectBest(candidates, requirement)`.
  - Construct `UtilityLocation(executablePath=selected, version=resolvedOrRequirement, platformType=PlatformDetector.current)`.
  - If none found, delegate to `PathEnvironmentLocation` as fallback.

### PlatformUtilityContext behavior
- Pass `latest` by default for `EDT_CLI`.
- For platform utilities, if version is null, prefer `latest` unless caller requires exact.

### UtilityLocator caching
- Preserve existing cache key `(utility, versionInput)`.
- Cache only successful resolutions.

### OS-specific bases (from task)
- Platform Linux: `/opt/1cv8/x86_64`, `/usr/local/1cv8/<ver>`, `/opt/1cv8/arm64/<ver>`, `/opt/1cv8/e2kv4/<ver>`, `/opt/1cv8/i386/<ver>`
- Platform Windows: `%PROGRAMFILES%/1cv8/<ver>/bin`, `%PROGRAMFILES(x86)%/1cv8/<ver>/bin`, `%LOCALAPPDATA%/Programs/1cv8/<ver>/bin`, `%LOCALAPPDATA%/Programs/1cv8_x86/<ver>/bin`, `%LOCALAPPDATA%/Programs/1cv8_x64/<ver>/bin`
- EDT Linux:
  - `/opt/1C/1CE/components/1c-edt-<ver>-<arch>/`
  - `~/.local/share/1C/1cedtstart/installations/1C_EDT <ver>/1cedt/`
- EDT Windows:
  - `%PROGRAMFILES%/1C/1CE/components/1c-edt-<ver>-<arch>/1cedt/`
  - `%LOCALAPPDATA%/1C/1cedtstart/installations/1C_EDT <ver>/1cedt/`
- EDT macOS: `/opt/1C/1CE/components/1c-edt-<ver>-<arch>/1cedt/`

### Regex suggestions
- Platform version dir: `^(?<ver>\\d+\\.\\d+\\.\\d+(?:\\.\\d+)?)$`
- EDT components dir: `^1c-edt-(?<ver>[0-9]{4}\\.[0-9]+\\.[0-9]+(?:\\+\\d+)?)-(?<arch>.+)$`
- EDT user installs: `^1C_EDT (?<ver>[0-9]{4}\\.[0-9]+(?:\\.[0-9]+)?)$`

### Minimal type sketches
```kotlin
// Version.kt
data class Version(val parts: List<Int>) : Comparable<Version> {
  override fun compareTo(other: Version): Int = TODO()
  fun startsWith(mask: Version): Boolean = TODO()
  companion object { fun parse(input: String): Version? = TODO() }
}

// VersionResolver.kt
interface VersionResolver {
  fun selectBest(candidates: List<Pair<Path, String?>>, requirement: String?): Path?
}
```

```kotlin
// DirectoryEnumeratingLocation.kt
class DirectoryEnumeratingLocation(
  private val basePath: Path,
  private val relativeExecutableSubPath: String,
  private val dirPatternRegex: Regex
) : SearchLocation {
  fun enumerateCandidates(utility: UtilityType): List<Pair<Path, String?>> = TODO()
}
```

## Testing Strategy
- Unit tests:
  - `VersionResolver` parse and compare for platform and EDT.
  - `DirectoryEnumeratingLocation` enumeration using temp directories.
- Integration tests:
  - `SearchStrategy.search()` for exact, mask, latest with simulated directory trees.
  - Ensure PATH fallback remains functional.
- Caching tests:
  - `UtilityLocator` returns cached results for repeated queries.

## Verification Checkpoint
- EDT CLI resolved by default with `latest`.
- Platform utilities resolved by exact, mask, or latest.
- Highest matching version selected for masked inputs.
- Works across Linux, Windows, macOS (in tests via simulated FS trees).
- Public API unchanged; caching behavior preserved.

# 🎨🎨🎨 EXITING CREATIVE PHASE

