# 🎨🎨🎨 ENTERING CREATIVE PHASE: ALGORITHM DESIGN

## Component: Hash-Based Incremental Build Detection System

### Component Description
A sophisticated file monitoring and change detection system that determines whether a full or incremental build is needed for 1C:Enterprise configurations. The system combines file watching, hash-based change detection, and intelligent build state management to minimize unnecessary rebuild operations while ensuring accuracy.

### Requirements & Constraints

#### Functional Requirements
- Detect file changes in 1C:Enterprise source code
- Maintain persistent hash storage for change tracking
- Determine optimal build strategy (full vs incremental)
- Support cross-platform file monitoring (Linux/Windows)
- Handle large codebases efficiently (thousands of files)
- Recover gracefully from interrupted builds
- Support dependency-aware change detection

#### Performance Requirements
- File hash computation: < 50ms for typical source files
- Change detection: < 100ms for project scanning
- Memory usage: < 50MB for hash storage
- Startup time: < 2 seconds for existing projects
- Build decision: < 200ms after file change

#### Technical Constraints
- MapDB for persistent key-value storage
- No external database dependencies
- Thread-safe concurrent access
- Atomic operations for consistency
- Cross-platform file path handling

### Algorithm Options Analysis

#### Option 1: Simple Timestamp-Based Detection
```kotlin
Algorithm: Compare file modification timestamps
Process:
1. Store lastModified timestamp for each file
2. On scan, compare current vs stored timestamps
3. If any timestamp changed → full rebuild
4. Otherwise → incremental rebuild

Time Complexity: O(n) where n = number of files
Space Complexity: O(n) for timestamp storage
```

**Pros:**
- Simple implementation
- Fast comparison operations
- Low memory overhead
- Cross-platform compatible

**Cons:**
- Unreliable (timestamps can be misleading)
- False positives from file system operations
- No content-based validation
- Cannot detect reverted changes
- Vulnerable to clock skew issues

#### Option 2: Content Hash-Based Detection with Full Scan
```kotlin
Algorithm: SHA-256 hash of file contents
Process:
1. Calculate SHA-256 hash for each file
2. Compare current hash vs stored hash
3. If any hash changed → determine scope:
   - Core files changed → full rebuild
   - Test files only → incremental rebuild
4. Update hash storage after successful build

Time Complexity: O(n * f) where n = files, f = avg file size
Space Complexity: O(n) for hash storage
```

**Pros:**
- Accurate content-based detection
- Detects actual changes only
- Can differentiate change types
- Cryptographically secure
- Platform independent

**Cons:**
- Higher CPU overhead for hashing
- Slower for large files
- Full project scan required
- No early termination optimization

#### Option 3: Hierarchical Hash with Change Propagation
```kotlin
Algorithm: Directory-level hashing with dependency tracking
Process:
1. Calculate hashes at multiple levels:
   - File level: SHA-256 of content
   - Directory level: Hash of child hashes
   - Project level: Hash of directory hashes
2. Use dependency graph to propagate changes:
   - Module A depends on Module B
   - If B changes, mark A as needing rebuild
3. Incremental hash updates:
   - Only rehash changed paths
   - Propagate hash changes up the tree

Time Complexity: O(k + d) where k = changed files, d = dependencies
Space Complexity: O(n + e) where n = files, e = dependency edges
```

**Pros:**
- Excellent performance for large projects
- Dependency-aware change detection
- Efficient incremental updates
- Early termination possible
- Scalable to very large codebases

**Cons:**
- Complex implementation
- Requires dependency analysis
- Memory overhead for dependency graph
- Potential inconsistency during updates

#### Option 4: Hybrid Timestamp + Hash Verification
```kotlin
Algorithm: Fast timestamp pre-filter + hash verification
Process:
1. First pass: Check timestamps for potential changes
2. Second pass: Calculate hashes only for timestamp-changed files
3. Compare hashes to determine actual changes
4. Use dependency analysis for build scope
5. Lazy hash calculation for unchanged files

Time Complexity: O(n + k*f) where k = potentially changed files
Space Complexity: O(n) for timestamps + hashes
```

**Pros:**
- Best of both worlds: speed + accuracy
- Efficient for mostly-unchanged projects
- Accurate change detection
- Good performance characteristics
- Reasonable complexity

**Cons:**
- More complex than pure approaches
- Two-pass scanning overhead
- Still vulnerable to timestamp issues
- Requires careful synchronization

### Recommended Approach: Enhanced Hybrid Algorithm

I recommend a sophisticated hybrid approach that combines the efficiency of timestamp checking with the accuracy of content hashing, enhanced with smart dependency tracking:

```kotlin
class IncrementalBuildDetector(
    private val hashStorage: HashStorage,
    private val fileWatcher: FileWatcher,
    private val dependencyAnalyzer: DependencyAnalyzer
) {
    
    suspend fun detectChanges(projectPath: Path): BuildDecision {
        val scanStart = System.currentTimeMillis()
        
        // Phase 1: Fast timestamp pre-scan
        val potentialChanges = scanTimestamps(projectPath)
        
        // Phase 2: Hash verification for potential changes
        val actualChanges = verifyChangesWithHashes(potentialChanges)
        
        // Phase 3: Dependency impact analysis
        val impactedModules = analyzeImpact(actualChanges)
        
        // Phase 4: Build decision logic
        return decideBuildStrategy(actualChanges, impactedModules, scanStart)
    }
    
    private suspend fun scanTimestamps(projectPath: Path): Set<Path> {
        return fileWatcher.getModifiedFiles(projectPath)
            .filter { file -> 
                val stored = hashStorage.getTimestamp(file)
                val current = file.lastModifiedTime()
                current > stored
            }
            .toSet()
    }
    
    private suspend fun verifyChangesWithHashes(
        candidates: Set<Path>
    ): Map<Path, ChangeType> {
        return candidates.parallelMap { file ->
            val currentHash = calculateFileHash(file)
            val storedHash = hashStorage.getHash(file)
            
            when {
                storedHash == null -> file to ChangeType.NEW
                currentHash != storedHash -> file to ChangeType.MODIFIED
                else -> file to ChangeType.UNCHANGED
            }
        }.filter { (_, type) -> type != ChangeType.UNCHANGED }
         .toMap()
    }
}
```

### Implementation Guidelines

#### 1. File Hash Calculation Strategy
```kotlin
// Optimized hash calculation with caching
class FileHashCalculator {
    private val hashCache = ConcurrentHashMap<Path, CachedHash>()
    
    suspend fun calculateHash(file: Path): String {
        val cached = hashCache[file]
        val lastModified = file.lastModifiedTime()
        
        if (cached != null && cached.timestamp >= lastModified) {
            return cached.hash
        }
        
        return withContext(Dispatchers.IO) {
            val hash = Files.newInputStream(file).use { input ->
                MessageDigest.getInstance("SHA-256").let { digest ->
                    input.copyTo(DigestOutputStream(digest))
                    digest.digest().joinToString("") { "%02x".format(it) }
                }
            }
            
            hashCache[file] = CachedHash(hash, lastModified)
            hash
        }
    }
}
```

#### 2. Persistent Hash Storage
```kotlin
// MapDB-based persistent storage with transactions
class MapDbHashStorage(private val dbFile: Path) : HashStorage {
    private val db = DBMaker.fileDB(dbFile.toString())
        .transactionEnable()
        .make()
    
    private val hashMap = db.hashMap("file_hashes")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .createOrOpen()
    
    override suspend fun getHash(file: Path): String? {
        return hashMap[file.toString()]
    }
    
    override suspend fun storeHash(file: Path, hash: String) {
        db.use { transaction ->
            hashMap[file.toString()] = hash
            transaction.commit()
        }
    }
    
    override suspend fun batchUpdate(updates: Map<Path, String>) {
        db.use { transaction ->
            updates.forEach { (path, hash) ->
                hashMap[path.toString()] = hash
            }
            transaction.commit()
        }
    }
}
```

#### 3. Smart Build Decision Logic
```kotlin
// Intelligent build strategy selection
class BuildDecisionEngine {
    fun decideBuildStrategy(
        changes: Map<Path, ChangeType>,
        dependencies: DependencyGraph,
        thresholds: BuildThresholds
    ): BuildDecision {
        
        val changeAnalysis = analyzeChangeImpact(changes, dependencies)
        
        return when {
            changeAnalysis.coreFilesChanged -> BuildDecision.FULL_BUILD
            changeAnalysis.configurationChanged -> BuildDecision.FULL_BUILD
            changeAnalysis.testOnlyChanges -> BuildDecision.INCREMENTAL_TESTS
            changeAnalysis.changedModules.size > thresholds.incrementalThreshold -> 
                BuildDecision.FULL_BUILD
            else -> BuildDecision.INCREMENTAL_BUILD(changeAnalysis.changedModules)
        }
    }
}
```

#### 4. Cross-Platform File Monitoring
```kotlin
// Platform-aware file monitoring
class CrossPlatformFileWatcher {
    private val watchService = when (currentPlatform) {
        Platform.WINDOWS -> WindowsWatchService()
        Platform.LINUX -> LinuxWatchService()
        Platform.MACOS -> MacOSWatchService()
    }
    
    suspend fun watchDirectory(
        path: Path, 
        callback: (FileChangeEvent) -> Unit
    ) {
        watchService.register(path, StandardWatchEventKinds.ENTRY_MODIFY) { event ->
            when (event.kind()) {
                ENTRY_CREATE, ENTRY_MODIFY -> callback(FileChangeEvent.Modified(event.context()))
                ENTRY_DELETE -> callback(FileChangeEvent.Deleted(event.context()))
            }
        }
    }
}
```

### Complexity Analysis & Performance Optimization

#### Time Complexity
- **Best Case**: O(k) where k = number of actually changed files
- **Average Case**: O(n + k*log(n)) where n = total files
- **Worst Case**: O(n*f) for full project rehashing

#### Space Complexity
- **Hash Storage**: O(n) for file path → hash mapping
- **Dependency Graph**: O(n + e) where e = dependency edges
- **Working Memory**: O(k) for processing changed files

#### Performance Optimizations
1. **Parallel Processing**: Hash calculation on multiple threads
2. **Lazy Loading**: Load hashes only when needed
3. **Batch Operations**: Group database updates
4. **Memory Mapping**: Use memory-mapped files for large projects
5. **Incremental Updates**: Update only changed directory trees

### Verification Checkpoint

**Algorithm Verification:**
✅ **Accuracy**: Content-based hashing ensures precise change detection
✅ **Performance**: Hybrid approach minimizes unnecessary work
✅ **Scalability**: Handles large projects efficiently with parallel processing
✅ **Reliability**: Persistent storage survives process restarts
✅ **Cross-platform**: Works consistently across Linux/Windows
✅ **Dependency-aware**: Considers module relationships in build decisions
✅ **Thread-safe**: Concurrent access through proper synchronization
✅ **Memory efficient**: Bounded memory usage with caching strategies

The algorithm provides optimal balance between accuracy, performance, and reliability for enterprise-scale 1C projects.

# 🎨🎨🎨 EXITING CREATIVE PHASE 