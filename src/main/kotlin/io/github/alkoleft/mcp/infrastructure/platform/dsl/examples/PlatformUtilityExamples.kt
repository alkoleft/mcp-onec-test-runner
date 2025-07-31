package io.github.alkoleft.mcp.infrastructure.platform.dsl.examples

import io.github.alkoleft.mcp.infrastructure.platform.dsl.PlatformUtilityDsl
import io.github.alkoleft.mcp.infrastructure.platform.dsl.configurator.ConnectionSpeed
import java.nio.file.Paths

/**
 * Примеры использования DSL для работы с утилитами платформы 1С
 */
class PlatformUtilityExamples(
    private val platformDsl: PlatformUtilityDsl
) {

    /**
     * Пример работы с конфигуратором
     */
    fun configuratorExample() {
        // Загрузка конфигурации из каталога с файлами исходников
        val loadFromFilesResult = platformDsl.configurator("8.3.24.1482") {
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
        }.loadFromFiles(Paths.get("/path/to/source/files"))

        println("Load from files result: ${loadFromFilesResult.success}")
        println("Output: ${loadFromFilesResult.output}")

        // Загрузка основной конфигурации из файла
        val loadMainResult = platformDsl.configurator("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/main_config.log"))
        }.loadMainConfig(Paths.get("/path/to/main_config.cf"))

        println("Load main config result: ${loadMainResult.success}")

        // Загрузка расширения конфигурации
        val loadExtensionResult = platformDsl.configurator("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/extension.log"))
        }.loadExtension(Paths.get("/path/to/extension.cfe"))

        println("Load extension result: ${loadExtensionResult.success}")

        // Проверка возможности применения расширений
        val checkExtensionsResult = platformDsl.configurator("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
        }.checkCanApplyExtensions()

        println("Check extensions result: ${checkExtensionsResult.success}")

        // Обновление конфигурации в информационной базе
        val updateResult = platformDsl.configurator("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/update.log"))
        }.updateDatabaseConfig()

        println("Update database config result: ${updateResult.success}")

        // Проверка конфигурации
        val checkConfigResult = platformDsl.configurator("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/check_config.log"))
        }.checkConfig()

        println("Check config result: ${checkConfigResult.success}")

        // Проверка модулей конфигурации
        val checkModulesResult = platformDsl.configurator("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/check_modules.log"))
        }.checkModules()

        println("Check modules result: ${checkModulesResult.success}")

        // Произвольная команда
        val customResult = platformDsl.configurator("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
        }.command("DumpCfg")

        println("Custom command result: ${customResult.success}")
    }

    /**
     * Пример комплексной работы с конфигурацией
     */
    fun complexConfiguratorExample() {
        // 1. Загружаем основную конфигурацию
        val mainConfigResult = platformDsl.configurator("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/main_config.log"))
            disableStartupDialogs()
        }.loadMainConfig(Paths.get("/path/to/main_config.cf"))

        if (mainConfigResult.success) {
            println("Основная конфигурация загружена успешно")

            // 2. Загружаем расширение
            val extensionResult = platformDsl.configurator("8.3.24.1482") {
                connect("Srvr=localhost;Ref=MyDatabase;")
                user("Administrator")
                password("password")
                output(Paths.get("/path/to/extension.log"))
            }.loadExtension(Paths.get("/path/to/extension.cfe"))

            if (extensionResult.success) {
                println("Расширение загружено успешно")

                // 3. Проверяем возможность применения расширений
                val checkExtensionsResult = platformDsl.configurator("8.3.24.1482") {
                    connect("Srvr=localhost;Ref=MyDatabase;")
                    user("Administrator")
                    password("password")
                }.checkCanApplyExtensions()

                if (checkExtensionsResult.success) {
                    println("Расширения можно применить")

                    // 4. Обновляем конфигурацию в базе
                    val updateResult = platformDsl.configurator("8.3.24.1482") {
                        connect("Srvr=localhost;Ref=MyDatabase;")
                        user("Administrator")
                        password("password")
                        output(Paths.get("/path/to/update.log"))
                    }.updateDatabaseConfig()

                    if (updateResult.success) {
                        println("Конфигурация обновлена успешно")

                        // 5. Проверяем конфигурацию
                        val checkResult = platformDsl.configurator("8.3.24.1482") {
                            connect("Srvr=localhost;Ref=MyDatabase;")
                            user("Administrator")
                            password("password")
                            output(Paths.get("/path/to/final_check.log"))
                        }.checkConfig()

                        if (checkResult.success) {
                            println("Конфигурация проверена успешно")
                        } else {
                            println("Ошибка проверки конфигурации: ${checkResult.error}")
                        }
                    } else {
                        println("Ошибка обновления конфигурации: ${updateResult.error}")
                    }
                } else {
                    println("Ошибка проверки расширений: ${checkExtensionsResult.error}")
                }
            } else {
                println("Ошибка загрузки расширения: ${extensionResult.error}")
            }
        } else {
            println("Ошибка загрузки основной конфигурации: ${mainConfigResult.error}")
        }
    }

    /**
     * Пример работы с ibcmd
     */
    fun ibcmdExample() {
        // Создание информационной базы
        val createResult = platformDsl.ibcmd("8.3.24.1482") {
            connect("Srvr=localhost;Ref=NewDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/create.log"))
        }.create()

        println("Create result: ${createResult.success}")

        // Получение списка информационных баз
        val listResult = platformDsl.ibcmd("8.3.24.1482") {
            connect("Srvr=localhost;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/list.txt"))
        }.list()

        println("List result: ${listResult.success}")
        println("Output: ${listResult.output}")

        // Проверка информационной базы
        val checkResult = platformDsl.ibcmd("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/check.log"))
        }.check()

        println("Check result: ${checkResult.success}")

        // Обновление информационной базы
        val updateResult = platformDsl.ibcmd("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/update.log"))
        }.update()

        println("Update result: ${updateResult.success}")

        // Сжатие информационной базы
        val compressResult = platformDsl.ibcmd("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
            output(Paths.get("/path/to/compress.log"))
        }.compress()

        println("Compress result: ${compressResult.success}")

        // Произвольная команда
        val customResult = platformDsl.ibcmd("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
        }.command("LOCKINFOBASE")

        println("Custom command result: ${customResult.success}")
    }

    /**
     * Пример комплексной работы с платформой
     */
    fun complexExample() {
        // Проверяем доступность утилит
        val platformResult = platformDsl.platform("8.3.24.1482") {
            // Контекст автоматически проверяет доступность утилит
        }

        println("Platform check result: ${platformResult.success}")

        // Создаем информационную базу и загружаем конфигурацию
        val ibResult = platformDsl.ibcmd("8.3.24.1482") {
            connect("Srvr=localhost;Ref=TestDatabase;")
            user("Administrator")
            password("password")
        }.create()

        if (ibResult.success) {
            val configResult = platformDsl.configurator("8.3.24.1482") {
                connect("Srvr=localhost;Ref=TestDatabase;")
                user("Administrator")
                password("password")
            }.loadFromFiles(Paths.get("/path/to/source/files"))

            println("Configuration load result: ${configResult.success}")
        }
    }

    /**
     * Пример обработки ошибок
     */
    fun errorHandlingExample() {
        try {
            val result = platformDsl.configurator("8.3.24.1482") {
                connect("Srvr=invalid;Ref=invalid;")
                user("Administrator")
                password("password")
            }.loadFromFiles(Paths.get("/invalid/path"))

            if (!result.success) {
                println("Error: ${result.error}")
                println("Exit code: ${result.exitCode}")
            }

        } catch (e: Exception) {
            println("Exception: ${e.message}")
        }
    }

    /**
     * Пример работы с синхронным API
     */
    fun syncExample() {
        // Синхронная проверка платформы
        val platformResult = platformDsl.platformSync("8.3.24.1482") {
            // Синхронная проверка доступности утилит
        }

        println("Sync platform check: ${platformResult.success}")

        // Синхронная команда конфигуратора
        val configResult = platformDsl.configurator("8.3.24.1482") {
            connect("Srvr=localhost;Ref=MyDatabase;")
            user("Administrator")
            password("password")
        }.commandSync("CheckConfig")

        println("Sync config command: ${configResult.success}")
    }
} 