## Task: Refactor utility search algorithm (Level 3)

### Overview
Refactor the utility discovery to support both 1C Platform and EDT tools with:
- Version masks (e.g., 8.3.20 → choose the highest matching 8.3.20.x)
- latest selection (default for EDT)
- Discover `EDT_CLI` across OS-specific locations
- Keep PATH fallback and caching

### Requirements
- Support exact version, masked version prefix, and keyword latest
- Cross-platform: Linux, Windows, macOS
- Support utilities: DESIGNER, THIN_CLIENT, THICK_CLIENT, IBCMD, IBSRV, EDT_CLI
- Prefer the best match when multiple candidates exist
- Non-invasive: preserve current public API

### Components Affected
- `infrastructure/platform/search/SearchLocation.kt`
- `infrastructure/platform/search/SearchStrategy.kt`
- `infrastructure/platform/locator/UtilityLocator.kt`
- `infrastructure/platform/locator/UtilityValidator.kt` (no API change)
- `infrastructure/platform/dsl/common/PlatformUtilityContext.kt` (behavioral: latest for EDT)
- New: `infrastructure/platform/search/VersionResolver.kt` (parser + matcher)

### Architecture Considerations
- Generalize search to enumerate directories and select version via a resolver.
- Keep `SearchLocation` simple; add a flexible location capable of scanning subdirectories.
- Introduce `VersionResolver` with:
  - Version parsing for Platform: major.minor.patch.build (e.g., 8.3.24.1234)
  - Version parsing for EDT: yyyy.M.m(+build) (e.g., 2025.1.0+656)
  - Matching: exact, prefix-mask, latest
  - Comparison: lexicographic over numeric segments; ignore non-numeric suffixes
- Use caching in `UtilityLocator` unchanged; cache key remains (utility, versionInput)

### OS-specific search bases
- Platform Linux: `/opt/1cv8/x86_64`, `/usr/local/1cv8/<ver>`, `/opt/1cv8/arm64/<ver>`, `/opt/1cv8/e2kv4/<ver>`, `/opt/1cv8/i386/<ver>`
- Platform Windows: `%PROGRAMFILES%/1cv8/<ver>/bin`, `%PROGRAMFILES(x86)%/1cv8/<ver>/bin`, `%LOCALAPPDATA%/Programs/1cv8/<ver>/bin`, `%LOCALAPPDATA%/Programs/1cv8_x86/<ver>/bin`, `%LOCALAPPDATA%/Programs/1cv8_x64/<ver>/bin`
- Platform Linux: `/opt/1cv8/x86_64`, `/usr/local/1cv8/<ver>`, `/opt/1cv8/arm64/<ver>`, `/opt/1cv8/e2kv4/<ver>`, `/opt/1cv8/i386/<ver>`
- EDT Linux:
  - `/opt/1C/1CE/components/1c-edt-<ver>-<arch>/`
  - `~/.local/share/1C/1cedtstart/installations/1C_EDT <ver>/1cedt/`
- EDT Windows:
  - `%PROGRAMFILES%/1C/1CE/components/1c-edt-<ver>-<arch>/1cedt/`
  - `%LOCALAPPDATA%/1C/1cedtstart/installations/1C_EDT <ver>/1cedt/`
- EDT macOS: `/opt/1C/1CE/components/1c-edt-<ver>-<arch>/1cedt/`

### Implementation Steps
1. Create `VersionResolver`:
   - `parsePlatformVersion(String): IntArray`
   - `parseEdtVersion(String): IntArray`
   - `extractVersionFromPath(Path, UtilityType): String?`
   - `selectBest(candidates: List<Pair<Path,String?>>, requirement: String?): Path`
   - Mask rule: treat requirement like prefix over parsed segments; latest selects max
2. Add `DirectoryEnumeratingLocation` implementing `SearchLocation`:
   - Parameters: `basePath`, `relativeExecutableSubPath` (e.g., `bin`, `1cedt`), `dirPatternRegex` for version extraction
   - Enumerate subdirectories, compose candidate paths for `getExecutableName(utility)` under `relativeExecutableSubPath`
3. Replace `VersionLocation` usage in strategies with `DirectoryEnumeratingLocation` where needed:
   - Platform strategies: enumerate `<base>/<version>/bin/1cv8[.exe]`
   - EDT strategies: enumerate both components and user installations; subpath `1cedt`
   - Keep `PathEnvironmentLocation` as a low-priority fallback
4. Update `search()`:
   - Gather candidates from all `locations`
   - Use `VersionResolver.selectBest` against `version` input (exact/mask/latest)
   - Validate executable exists and is executable before selection
5. `UtilityLocator`:
   - Keep API; ensure `version` can be a mask or latest
   - Cache successful resolutions
6. Update `PlatformUtilityContext` (already passes `latest` for EDT)

### Testing Strategy
- Unit tests for `VersionResolver` (platform and EDT variants)
- Unit tests for `DirectoryEnumeratingLocation` using temp dirs
- Integration tests for `SearchStrategy.search()` behavior:
  - exact, mask, latest
  - EDT paths on Linux-like temp structure
  - PATH fallback
- Validate caching behavior remains correct

### Potential Challenges & Mitigations
- Version parsing across heterogeneous names → centralize regex and parsing in `VersionResolver`
- Performance of directory scans → restrict to known bases; short-circuit on early best match; cache results
- Home expansion for `~` → normalize via `user.home`
- Windows env variability → guard null env vars and skip silently

### Acceptance Criteria
- EDT CLI (`EDT_CLI`) resolved by default with `latest`
- Platform utilities resolved by exact version, prefix-mask, or latest
- Highest matching version is selected for masked inputs
- Works on Linux, Windows, macOS in tests

### Reference Examples (EDT)
- `/home/<user>/.local/share/1C/1cedtstart/installations/1C_EDT 2023.2/1cedt/`
- `/opt/1C/1CE/components/1c-edt-2023.2.4+6-x86_64/1cedt/`
- `/home/<user>/.local/share/1C/1cedtstart/installations/1C_EDT 2022.2/1cedt/`
- `/opt/1C/1CE/components/1c-edt-2025.1.0+656-x86_64/1cedt/`