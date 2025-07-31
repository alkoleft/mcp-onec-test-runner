package io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator

import io.github.alkoleft.mcp.infrastructure.platform.dsl.common.PlatformUtilityContext
import java.nio.file.Path

/**
 * DSL для формирования плана команд конфигуратора 1С
 *
 * Позволяет создать план команд с описанием, который можно показать пользователю
 * и затем последовательно выполнить
 */
class ConfiguratorPlanDsl(
    private val context: PlatformUtilityContext
) {
    private val configuratorContext = ConfiguratorContext(context)
    private val commands = mutableListOf<ConfiguratorCommand>()

    /**
     * Устанавливает строку подключения к информационной базе
     */
    fun connect(connectionString: String) {
        configuratorContext.connect(connectionString)
    }

    /**
     * Устанавливает строку подключения к серверной информационной базе
     */
    fun connectToServer(serverName: String, dbName: String) {
        configuratorContext.connectToServer(serverName, dbName)
    }

    /**
     * Устанавливает строку подключения к файловой информационной базе
     */
    fun connectToFile(path: String) {
        configuratorContext.connectToFile(path)
    }

    /**
     * Устанавливает пользователя для подключения
     */
    fun user(user: String) {
        configuratorContext.user(user)
    }

    /**
     * Устанавливает пароль для подключения
     */
    fun password(password: String) {
        configuratorContext.password(password)
    }

    /**
     * Устанавливает путь к конфигурации (для операций с файлами .cf)
     */
    fun config(path: Path) {
        configuratorContext.config(path)
    }

    /**
     * Устанавливает путь для вывода
     */
    fun output(path: Path) {
        configuratorContext.output(path)
    }

    /**
     * Устанавливает путь для лог-файла
     */
    fun log(path: Path) {
        configuratorContext.log(path)
    }

    /**
     * Устанавливает код языка интерфейса
     */
    fun language(code: String) {
        configuratorContext.language(code)
    }

    /**
     * Устанавливает код локализации сеанса
     */
    fun localization(code: String) {
        configuratorContext.localization(code)
    }

    /**
     * Устанавливает скорость соединения
     */
    fun connectionSpeed(speed: ConnectionSpeed) {
        configuratorContext.connectionSpeed(speed)
    }

    /**
     * Отключает диалоговые окна запуска
     */
    fun disableStartupDialogs() {
        configuratorContext.disableStartupDialogs()
    }

    /**
     * Отключает сообщения запуска
     */
    fun disableStartupMessages() {
        configuratorContext.disableStartupMessages()
    }

    /**
     * Не очищает файл вывода при записи
     */
    fun noTruncate() {
        configuratorContext.noTruncate()
    }

    /**
     * Добавляет дополнительные параметры
     */
    fun param(param: String) {
        configuratorContext.param(param)
    }

    /**
     * Добавляет команду LoadConfigFromFiles в план
     */
    fun loadConfigFromFiles(block: LoadConfigFromFilesPlanDsl.() -> Unit) {
        val dsl = LoadConfigFromFilesPlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Добавляет команду UpdateDBCfg в план
     */
    fun updateDBCfg(block: UpdateDBCfgPlanDsl.() -> Unit) {
        val dsl = UpdateDBCfgPlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Добавляет команду CheckCanApplyConfigurationExtensions в план
     */
    fun checkCanApplyConfigurationExtensions(block: CheckCanApplyConfigurationExtensionsPlanDsl.() -> Unit) {
        val dsl = CheckCanApplyConfigurationExtensionsPlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Добавляет команду CheckConfig в план
     */
    fun checkConfig(block: CheckConfigPlanDsl.() -> Unit) {
        val dsl = CheckConfigPlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Добавляет команду CheckModules в план
     */
    fun checkModules(block: CheckModulesPlanDsl.() -> Unit) {
        val dsl = CheckModulesPlanDsl()
        dsl.block()
        commands.add(dsl.buildCommand())
    }

    /**
     * Строит план выполнения команд
     */
    fun buildPlan(): ConfiguratorPlan {
        return ConfiguratorPlan(commands.toList(), configuratorContext)
    }
}

/**
 * DSL для планирования команды LoadConfigFromFiles
 *
 * /LoadConfigFromFiles <каталог загрузки> [-Extension <имя расширения>]
 * [-AllExtensions][-files "<файлы>"][-partial][-listFile <файл списка>][-format <режим>]
 * [-updateConfigDumpInfo][-NoCheck][-Archive <имя ZIP-архива>]
 */
class LoadConfigFromFilesPlanDsl {
    /**
     * Путь к каталогу, содержащему XML-файлы конфигурации (обязательный параметр)
     */
    var sourcePath: Path? = null

    /**
     * Имя расширения для обработки
     * Если расширение успешно обработано возвращает код возврата 0, в противном случае — 1
     */
    var extension: String? = null

    /**
     * Загрузка только расширений (всех)
     * Для каждого подкаталога указанного каталога будет выполнена попытка создать расширение
     */
    var allExtensions: Boolean = false

    /**
     * Список файлов, которые требуется загрузить
     * Список разделяется запятыми. Не используется, если указан параметр listFile
     */
    var files: String? = null

    /**
     * Частичная загрузка
     * Загрузка только файлов, указанных в параметре files или listFile
     */
    var partial: Boolean = false

    /**
     * Файл со списком файлов для загрузки
     * Файл должен быть в кодировке UTF-8, имена файлов через перенос строки
     */
    var listFile: Path? = null

    /**
     * Формат загрузки файлов
     * Используется для частичной загрузки (Hierarchical или Plain)
     */
    var format: LoadFormat? = null

    /**
     * Создание файла версий ConfigDumpInfo.xml
     * В конце загрузки в каталоге будет создан файл версий, соответствующий загруженной конфигурации
     */
    var updateConfigDumpInfo: Boolean = false

    /**
     * Отключение проверки целостности загружаемой конфигурации
     */
    var noCheck: Boolean = false

    /**
     * ZIP-архив для загрузки
     * Для успешного выполнения команды требуется либо каталог загрузки, либо данный параметр
     */
    var archive: Path? = null

    /**
     * Устанавливает каталог загрузки (обязательный параметр)
     * @param path путь к каталогу, содержащему XML-файлы конфигурации
     */
    fun fromPath(path: Path) {
        this.sourcePath = path
    }

    /**
     * Устанавливает расширение для обработки
     * @param name имя расширения
     */
    fun extension(name: String) {
        this.extension = name
    }

    /**
     * Включает загрузку всех расширений
     * Для каждого подкаталога указанного каталога будет выполнена попытка создать расширение
     */
    fun allExtensions() {
        this.allExtensions = true
    }

    /**
     * Устанавливает список файлов для загрузки
     * @param fileList список файлов, разделенных запятыми
     */
    fun files(fileList: String) {
        this.files = fileList
    }

    /**
     * Включает частичную загрузку
     * Загрузка только файлов, указанных в параметре -files или -listFile
     */
    fun partial() {
        this.partial = true
    }

    /**
     * Устанавливает файл со списком файлов для загрузки
     * @param file путь к файлу со списком файлов (UTF-8, имена через перенос строки)
     */
    fun listFile(file: Path) {
        this.listFile = file
    }

    /**
     * Устанавливает формат загрузки файлов
     * @param format формат загрузки (Hierarchical или Plain)
     */
    fun format(format: LoadFormat) {
        this.format = format
    }

    /**
     * Включает создание файла версий ConfigDumpInfo.xml
     * В конце загрузки в каталоге будет создан файл версий, соответствующий загруженной конфигурации
     */
    fun updateConfigDumpInfo() {
        this.updateConfigDumpInfo = true
    }

    /**
     * Отключает проверку целостности загружаемой конфигурации
     */
    fun noCheck() {
        this.noCheck = true
    }

    /**
     * Устанавливает ZIP-архив для загрузки
     * @param archive путь к ZIP-архиву
     */
    fun archive(archive: Path) {
        this.archive = archive
    }

    /**
     * Строит команду LoadConfigFromFiles
     */
    fun buildCommand(): LoadConfigFromFilesCommand {
        return LoadConfigFromFilesCommand(
            sourcePath = sourcePath,
            extension = extension,
            allExtensions = allExtensions,
            files = files,
            partial = partial,
            listFile = listFile,
            format = format,
            updateConfigDumpInfo = updateConfigDumpInfo,
            noCheck = noCheck,
            archive = archive
        )
    }
}

/**
 * DSL для планирования команды UpdateDBCfg
 */
class UpdateDBCfgPlanDsl {
    private var backgroundStart: Boolean = false
    private var backgroundCancel: Boolean = false
    private var backgroundFinish: Boolean = false
    private var backgroundResume: Boolean = false
    private var backgroundSuspend: Boolean = false
    private var dynamicMode: DynamicMode? = null
    private var visible: Boolean = false
    private var warningsAsErrors: Boolean = false
    private var server: Boolean = false
    private var extension: String? = null
    private var sessionTerminate: SessionTerminateMode? = null

    /**
     * Запускает фоновое обновление конфигурации
     */
    fun backgroundStart(mode: DynamicMode = DynamicMode.PLUS) {
        this.backgroundStart = true
        this.dynamicMode = mode
    }

    /**
     * Отменяет фоновое обновление
     */
    fun backgroundCancel() {
        this.backgroundCancel = true
    }

    /**
     * Завершает фоновое обновление
     */
    fun backgroundFinish(visible: Boolean = false) {
        this.backgroundFinish = true
        this.visible = visible
    }

    /**
     * Возобновляет фоновое обновление
     */
    fun backgroundResume() {
        this.backgroundResume = true
    }

    /**
     * Приостанавливает фоновое обновление
     */
    fun backgroundSuspend() {
        this.backgroundSuspend = true
    }

    /**
     * Устанавливает режим динамического обновления
     */
    fun dynamicMode(mode: DynamicMode) {
        this.dynamicMode = mode
    }

    /**
     * Показывает диалоговое окно при завершении
     */
    fun visible() {
        this.visible = true
    }

    /**
     * Трактует предупреждения как ошибки
     */
    fun warningsAsErrors() {
        this.warningsAsErrors = true
    }

    /**
     * Выполняет обновление на сервере
     */
    fun server() {
        this.server = true
    }

    /**
     * Устанавливает расширение для обработки
     */
    fun extension(name: String) {
        this.extension = name
    }

    /**
     * Устанавливает режим завершения сеансов
     */
    fun sessionTerminate(mode: SessionTerminateMode) {
        this.sessionTerminate = mode
    }

    /**
     * Строит команду UpdateDBCfg
     */
    fun buildCommand(): UpdateDBCfgCommand {
        return UpdateDBCfgCommand(
            backgroundStart = backgroundStart,
            backgroundCancel = backgroundCancel,
            backgroundFinish = backgroundFinish,
            backgroundResume = backgroundResume,
            backgroundSuspend = backgroundSuspend,
            dynamicMode = dynamicMode,
            visible = visible,
            warningsAsErrors = warningsAsErrors,
            server = server,
            extension = extension,
            sessionTerminate = sessionTerminate
        )
    }
}

/**
 * DSL для планирования команды CheckCanApplyConfigurationExtensions
 */
class CheckCanApplyConfigurationExtensionsPlanDsl {
    private var extension: String? = null
    private var allZones: Boolean = false
    private var zones: String? = null

    /**
     * Устанавливает расширение для проверки
     */
    fun extension(name: String) {
        this.extension = name
    }

    /**
     * Проверяет расширения во всех областях информационной базы
     */
    fun allZones() {
        this.allZones = true
    }

    /**
     * Устанавливает разделители для проверки
     */
    fun zones(separators: String) {
        this.zones = separators
    }

    /**
     * Строит команду CheckCanApplyConfigurationExtensions
     */
    fun buildCommand(): CheckCanApplyConfigurationExtensionsCommand {
        return CheckCanApplyConfigurationExtensionsCommand(
            extension = extension,
            allZones = allZones,
            zones = zones
        )
    }
}

/**
 * DSL для планирования команды CheckConfig
 */
class CheckConfigPlanDsl {
    private var configLogIntegrity: Boolean = false
    private var incorrectReferences: Boolean = false
    private var thinClient: Boolean = false
    private var webClient: Boolean = false
    private var mobileClient: Boolean = false
    private var server: Boolean = false
    private var externalConnection: Boolean = false
    private var externalConnectionServer: Boolean = false
    private var mobileAppClient: Boolean = false
    private var mobileAppServer: Boolean = false
    private var thickClientManagedApplication: Boolean = false
    private var thickClientServerManagedApplication: Boolean = false
    private var thickClientOrdinaryApplication: Boolean = false
    private var thickClientServerOrdinaryApplication: Boolean = false
    private var mobileClientDigiSign: Boolean = false
    private var distributiveModules: Boolean = false
    private var unreferenceProcedures: Boolean = false
    private var handlersExistence: Boolean = false
    private var emptyHandlers: Boolean = false
    private var extendedModulesCheck: Boolean = false
    private var checkUseSynchronousCalls: Boolean = false
    private var checkUseModality: Boolean = false
    private var unsupportedFunctional: Boolean = false
    private var extension: String? = null
    private var allExtensions: Boolean = false

    /**
     * Проверка логической целостности конфигурации
     */
    fun configLogIntegrity() {
        this.configLogIntegrity = true
    }

    /**
     * Поиск некорректных ссылок
     */
    fun incorrectReferences() {
        this.incorrectReferences = true
    }

    /**
     * Синтаксический контроль модулей для тонкого клиента
     */
    fun thinClient() {
        this.thinClient = true
    }

    /**
     * Синтаксический контроль модулей для веб-клиента
     */
    fun webClient() {
        this.webClient = true
    }

    /**
     * Синтаксический контроль модулей для мобильного клиента
     */
    fun mobileClient() {
        this.mobileClient = true
    }

    /**
     * Синтаксический контроль модулей для сервера
     */
    fun server() {
        this.server = true
    }

    /**
     * Синтаксический контроль модулей для внешнего соединения
     */
    fun externalConnection() {
        this.externalConnection = true
    }

    /**
     * Синтаксический контроль модулей для внешнего соединения на сервере
     */
    fun externalConnectionServer() {
        this.externalConnectionServer = true
    }

    /**
     * Синтаксический контроль модулей для клиента мобильного приложения
     */
    fun mobileAppClient() {
        this.mobileAppClient = true
    }

    /**
     * Синтаксический контроль модулей для сервера мобильного приложения
     */
    fun mobileAppServer() {
        this.mobileAppServer = true
    }

    /**
     * Синтаксический контроль модулей для управляемого приложения (толстый клиент)
     */
    fun thickClientManagedApplication() {
        this.thickClientManagedApplication = true
    }

    /**
     * Синтаксический контроль модулей для управляемого приложения на сервере (толстый клиент)
     */
    fun thickClientServerManagedApplication() {
        this.thickClientServerManagedApplication = true
    }

    /**
     * Синтаксический контроль модулей для обычного приложения (толстый клиент)
     */
    fun thickClientOrdinaryApplication() {
        this.thickClientOrdinaryApplication = true
    }

    /**
     * Синтаксический контроль модулей для обычного приложения на сервере (толстый клиент)
     */
    fun thickClientServerOrdinaryApplication() {
        this.thickClientServerOrdinaryApplication = true
    }

    /**
     * Проверка корректности подписи мобильного клиента
     */
    fun mobileClientDigiSign() {
        this.mobileClientDigiSign = true
    }

    /**
     * Поставка модулей без исходных текстов
     */
    fun distributiveModules() {
        this.distributiveModules = true
    }

    /**
     * Поиск неиспользуемых процедур и функций
     */
    fun unreferenceProcedures() {
        this.unreferenceProcedures = true
    }

    /**
     * Проверка существования назначенных обработчиков
     */
    fun handlersExistence() {
        this.handlersExistence = true
    }

    /**
     * Поиск пустых обработчиков
     */
    fun emptyHandlers() {
        this.emptyHandlers = true
    }

    /**
     * Расширенная проверка модулей
     */
    fun extendedModulesCheck() {
        this.extendedModulesCheck = true
    }

    /**
     * Поиск использования синхронных методов
     */
    fun checkUseSynchronousCalls() {
        this.checkUseSynchronousCalls = true
    }

    /**
     * Поиск использования методов, связанных с модальностью
     */
    fun checkUseModality() {
        this.checkUseModality = true
    }

    /**
     * Поиск неподдерживаемой функциональности
     */
    fun unsupportedFunctional() {
        this.unsupportedFunctional = true
    }

    /**
     * Устанавливает расширение для проверки
     */
    fun extension(name: String) {
        this.extension = name
    }

    /**
     * Проверяет все расширения
     */
    fun allExtensions() {
        this.allExtensions = true
    }

    /**
     * Строит команду CheckConfig
     */
    fun buildCommand(): CheckConfigCommand {
        return CheckConfigCommand(
            configLogIntegrity = configLogIntegrity,
            incorrectReferences = incorrectReferences,
            thinClient = thinClient,
            webClient = webClient,
            mobileClient = mobileClient,
            server = server,
            externalConnection = externalConnection,
            externalConnectionServer = externalConnectionServer,
            mobileAppClient = mobileAppClient,
            mobileAppServer = mobileAppServer,
            thickClientManagedApplication = thickClientManagedApplication,
            thickClientServerManagedApplication = thickClientServerManagedApplication,
            thickClientOrdinaryApplication = thickClientOrdinaryApplication,
            thickClientServerOrdinaryApplication = thickClientServerOrdinaryApplication,
            mobileClientDigiSign = mobileClientDigiSign,
            distributiveModules = distributiveModules,
            unreferenceProcedures = unreferenceProcedures,
            handlersExistence = handlersExistence,
            emptyHandlers = emptyHandlers,
            extendedModulesCheck = extendedModulesCheck,
            checkUseSynchronousCalls = checkUseSynchronousCalls,
            checkUseModality = checkUseModality,
            unsupportedFunctional = unsupportedFunctional,
            extension = extension,
            allExtensions = allExtensions
        )
    }
}

/**
 * DSL для планирования команды CheckModules
 */
class CheckModulesPlanDsl {
    private var thinClient: Boolean = false
    private var webClient: Boolean = false
    private var server: Boolean = false
    private var externalConnection: Boolean = false
    private var thickClientOrdinaryApplication: Boolean = false
    private var mobileAppClient: Boolean = false
    private var mobileAppServer: Boolean = false
    private var mobileClient: Boolean = false
    private var extendedModulesCheck: Boolean = false
    private var extension: String? = null
    private var allExtensions: Boolean = false

    /**
     * Проверка в режиме работы тонкого клиента
     */
    fun thinClient() {
        this.thinClient = true
    }

    /**
     * Проверка в режиме работы веб-клиента
     */
    fun webClient() {
        this.webClient = true
    }

    /**
     * Проверка в режиме работы сервера 1С:Предприятия
     */
    fun server() {
        this.server = true
    }

    /**
     * Проверка в режиме работы внешнего соединения
     */
    fun externalConnection() {
        this.externalConnection = true
    }

    /**
     * Проверка в режиме работы клиентского приложения
     */
    fun thickClientOrdinaryApplication() {
        this.thickClientOrdinaryApplication = true
    }

    /**
     * Проверка в режиме работы клиента мобильного приложения
     */
    fun mobileAppClient() {
        this.mobileAppClient = true
    }

    /**
     * Проверка в режиме работы сервера мобильного приложения
     */
    fun mobileAppServer() {
        this.mobileAppServer = true
    }

    /**
     * Проверка в режиме работы мобильного клиента
     */
    fun mobileClient() {
        this.mobileClient = true
    }

    /**
     * Расширенная проверка модулей
     */
    fun extendedModulesCheck() {
        this.extendedModulesCheck = true
    }

    /**
     * Устанавливает расширение для проверки
     */
    fun extension(name: String) {
        this.extension = name
    }

    /**
     * Проверяет все расширения
     */
    fun allExtensions() {
        this.allExtensions = true
    }

    /**
     * Строит команду CheckModules
     */
    fun buildCommand(): CheckModulesCommand {
        return CheckModulesCommand(
            thinClient = thinClient,
            webClient = webClient,
            server = server,
            externalConnection = externalConnection,
            thickClientOrdinaryApplication = thickClientOrdinaryApplication,
            mobileAppClient = mobileAppClient,
            mobileAppServer = mobileAppServer,
            mobileClient = mobileClient,
            extendedModulesCheck = extendedModulesCheck,
            extension = extension,
            allExtensions = allExtensions
        )
    }
} 