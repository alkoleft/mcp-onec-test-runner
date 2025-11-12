# Пакет `io.github.alkoleft.mcp.infrastructure.storage`

## Назначение

Пакет `infrastructure.storage` содержит компоненты для хранения состояния сборки и отслеживания изменений в файловой системе проекта. Реализует алгоритм Enhanced Hybrid Hash Detection для оптимизации производительности.

## Основные компоненты

### FileBuildStateManager

**Класс:** `io.github.alkoleft.mcp.infrastructure.storage.FileBuildStateManager`

Менеджер состояния сборки, реализующий алгоритм Enhanced Hybrid Hash Detection. Комбинирует быстрое предварительное сканирование по временным меткам с точной проверкой хешей для оптимальной производительности.

**Методы:**
- `checkChanges(): ChangesSet` - проверяет изменения в файловой системе (suspend функция)
- `updateHashes(files: Map<Path, String>)` - обновляет хеши файлов
- `storeTimestamp(sourceSetName: String, timestamp: Long)` - сохраняет временную метку сборки
- `getTimestamp(sourceSetName: String): Long?` - получает временную метку последней сборки

**Алгоритм Enhanced Hybrid Hash Detection:**
1. **Фаза 1:** Быстрое сканирование временных меток для определения потенциально измененных файлов
2. **Фаза 2:** Проверка SHA-256 хешей для файлов с измененными временными метками
3. **Результат:** Только файлы с реальными изменениями

### MapDbHashStorage

**Класс:** `io.github.alkoleft.mcp.infrastructure.storage.MapDbHashStorage`

Хранилище хешей файлов на основе MapDB. Обеспечивает персистентное хранение хешей файлов между запусками приложения.

**Методы:**
- `getHash(file: Path): String?` - получает хеш файла
- `updateHash(file: Path, hash: String)` - обновляет хеш файла
- `batchUpdate(files: Map<Path, String>)` - массовое обновление хешей
- `removeHash(file: Path)` - удаляет хеш файла

### HashCalculator

**Класс:** `io.github.alkoleft.mcp.infrastructure.storage.HashCalculator`

Калькулятор хешей файлов. Вычисляет SHA-256 хеши файлов для отслеживания изменений.

**Методы:**
- `calculateHash(file: Path): String` - вычисляет хеш файла

## Связи с другими модулями

### Зависимости

- **MapDB** - используется для персистентного хранения
- **application.actions.change** - использует типы данных для изменений

### Используется в

- **application.actions.change.FileSystemChangeAnalysisAction** - использует для анализа изменений

