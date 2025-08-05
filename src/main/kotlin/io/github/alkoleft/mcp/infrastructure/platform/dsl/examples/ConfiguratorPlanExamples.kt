package io.github.alkoleft.mcp.infrastructure.platform.dsl.examples

import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.ConnectionSpeed
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.DynamicMode
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.LoadFormat
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.SessionTerminateMode
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.commands.LoadConfigFromFilesCommand
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

/**
 * Примеры использования DSL для формирования планов команд конфигуратора 1С
 */
class ConfiguratorPlanExamples(
    private val platformDsl: PlatformUtilityDsl
) {

    /**
     * Пример создания плана с загрузкой конфигурации и проверками
     */
    fun createLoadAndCheckPlan() {
        val planDsl = platformDsl.configuratorPlan("8.3.24.1482") {
            // Настройки подключения
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/output.txt"))
            log(Paths.get("/path/to/log.txt"))
            language("ru")
            localization("ru")
            connectionSpeed(ConnectionSpeed.NORMAL)
            disableStartupDialogs()
            disableStartupMessages()
            noTruncate()

            // Команда 1: Загрузка конфигурации из файлов
            loadConfigFromFiles {
                fromPath(Paths.get("/path/to/source/files"))
                extension("MyExtension")
                partial()
                format(LoadFormat.HIERARCHICAL)
                updateConfigDumpInfo()
                noCheck()
            }

            // Команда 2: Проверка применимости расширений
            checkCanApplyConfigurationExtensions {
                allZones()
            }

            // Команда 3: Проверка конфигурации
            checkConfig {
                configLogIntegrity()
                incorrectReferences()
                thinClient()
                webClient()
                server()
                mobileClient()
                distributiveModules()
                unreferenceProcedures()
                handlersExistence()
                emptyHandlers()
                extendedModulesCheck()
                checkUseSynchronousCalls()
                checkUseModality()
                unsupportedFunctional()
            }

            // Команда 4: Проверка модулей
            checkModules {
                allModules()
            }
        }

        // Строим план
        val plan = planDsl.buildPlan()

        // Показываем план пользователю
        plan.printPlan()

        // Выполняем план
        runBlocking {
            val results = plan.execute()

            // Анализируем результаты
            results.forEachIndexed { index, result ->
                println("Команда ${index + 1}: ${if (result.success) "УСПЕХ" else "ОШИБКА"}")
                if (!result.success) {
                    println("Ошибка: ${result.error}")
                }
                println("Время выполнения: ${result.duration.inWholeMilliseconds}ms")
                println("---")
            }
        }
    }

    /**
     * Пример создания плана обновления конфигурации
     */
    fun createUpdatePlan() {
        val planDsl = platformDsl.configuratorPlan("8.3.24.1482") {
            // Настройки подключения
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/update.log"))

            // Команда 1: Обновление конфигурации в информационной базе
            updateDBCfg {
                backgroundStart(DynamicMode.PLUS)
                warningsAsErrors()
                server()
                sessionTerminate(SessionTerminateMode.FORCE)
            }

            // Команда 2: Проверка конфигурации после обновления
            checkConfig {
                configLogIntegrity()
                incorrectReferences()
                server()
            }
        }

        val plan = planDsl.buildPlan()
        plan.printPlan()

        runBlocking {
            val results = plan.execute()
            results.forEachIndexed { index, result ->
                println("Результат команды ${index + 1}: ${if (result.success) "УСПЕХ" else "ОШИБКА"}")
            }
        }
    }

    /**
     * Пример создания плана проверки расширений
     */
    fun createExtensionCheckPlan() {
        val planDsl = platformDsl.configuratorPlan("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")

            // Проверка конкретного расширения
            checkCanApplyConfigurationExtensions {
                extension("MyExtension")
                allZones()
            }

            // Проверка конфигурации с расширением
            checkConfig {
                configLogIntegrity()
                thinClient()
                webClient()
                server()
                extension("MyExtension")
            }

            // Проверка модулей с расширением
            checkModules {
                extension("MyExtension")
            }
        }

        val plan = planDsl.buildPlan()
        plan.printPlan()

        runBlocking {
            val results = plan.execute()
            results.forEachIndexed { index, result ->
                println("Проверка расширения - команда ${index + 1}: ${if (result.success) "УСПЕХ" else "ОШИБКА"}")
            }
        }
    }

    /**
     * Пример создания плана с фоновым обновлением
     */
    fun createBackgroundUpdatePlan() {
        val planDsl = platformDsl.configuratorPlan("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")

            // Запуск фонового обновления
            updateDBCfg {
                backgroundStart(DynamicMode.PLUS)
                server()
            }

            // Завершение фонового обновления
            updateDBCfg {
                backgroundFinish(true) // с диалоговым окном
                server()
            }
        }

        val plan = planDsl.buildPlan()
        plan.printPlan()

        runBlocking {
            val results = plan.execute()
            results.forEachIndexed { index, result ->
                println("Фоновое обновление - команда ${index + 1}: ${if (result.success) "УСПЕХ" else "ОШИБКА"}")
            }
        }
    }

    /**
     * Пример создания плана с различными параметрами LoadConfigFromFiles
     */
    fun createLoadConfigFromFilesExamples() {
        val planDsl = platformDsl.configuratorPlan("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")

            // Пример 1: Загрузка всех расширений
            loadConfigFromFiles {
                fromPath(Paths.get("/path/to/config/source"))
                allExtensions()
                updateConfigDumpInfo()
            }

            // Пример 2: Загрузка конкретного расширения
            loadConfigFromFiles {
                fromPath(Paths.get("/path/to/extension/source"))
                extension("MyExtension")
                partial()
                format(LoadFormat.PLAIN)
            }

            // Пример 3: Загрузка из ZIP-архива
            loadConfigFromFiles {
                archive(Paths.get("/path/to/config.zip"))
                noCheck()
            }

            // Пример 4: Загрузка конкретных файлов
            loadConfigFromFiles {
                fromPath(Paths.get("/path/to/config/source"))
                files("CommonModules/MyModule/Ext/Module.bsl,CommonModules/MyModule/Ext/Module.xml")
                partial()
            }

            // Пример 5: Загрузка из файла списка
            loadConfigFromFiles {
                fromPath(Paths.get("/path/to/config/source"))
                listFile(Paths.get("/path/to/filelist.txt"))
                partial()
                format(LoadFormat.HIERARCHICAL)
            }

            // Пример 6: Использование свойств через присвоение
            loadConfigFromFiles {
                fromPath(Paths.get("/path/to/config/source"))
                extension("MyExtension")
                partial()
                format(LoadFormat.HIERARCHICAL)
                updateConfigDumpInfo()
            }

            // Пример 7: Смешанное использование методов и свойств
            loadConfigFromFiles {
                fromPath(Paths.get("/path/to/config/source"))
                extension("MyExtension")
                partial()
                format(LoadFormat.PLAIN)
                noCheck()
            }

            // Пример 8: Загрузка всех расширений через свойство
            loadConfigFromFiles {
                fromPath(Paths.get("/path/to/config/source"))
                allExtensions()
                updateConfigDumpInfo()
            }
        }

        val plan = planDsl.buildPlan()
        plan.printPlan()

        runBlocking {
            val results = plan.execute()
            results.forEachIndexed { index, result ->
                println("LoadConfigFromFiles пример ${index + 1}: ${if (result.success) "УСПЕХ" else "ОШИБКА"}")
            }
        }
    }

    /**
     * Примеры использования LoadConfigFromFilesCommand напрямую
     */
    fun createLoadConfigFromFilesCommandExamples() {
        // Пример 1: Создание команды через DSL методы
        val command1 = LoadConfigFromFilesCommand().apply {
            fromPath(Paths.get("/path/to/config/source"))
            extension("MyExtension")
            partial()
            format(LoadFormat.HIERARCHICAL)
        }

        // Пример 2: Создание команды напрямую
        val command2 = LoadConfigFromFilesCommand().apply {
            fromPath(Paths.get("/path/to/config/source"))
            allExtensions()
            updateConfigDumpInfo()
        }

        // Пример 3: Создание команды с архивом
        val command3 = LoadConfigFromFilesCommand().apply {
            fromPath(Paths.get("/path/to/config/source"))
            archive(Paths.get("/path/to/config.zip"))
            noCheck()
        }

        // Пример 4: Создание команды с файлами
        val command4 = LoadConfigFromFilesCommand().apply {
            fromPath(Paths.get("/path/to/config/source"))
            files("CommonModules/MyModule/Ext/Module.bsl,CommonModules/MyModule/Ext/Module.xml")
            partial()
        }

        println("Команда 1: ${command1.arguments}")
        println("Команда 2: ${command2.arguments}")
        println("Команда 3: ${command3.arguments}")
        println("Команда 4: ${command4.arguments}")
    }

    /**
     */
} 