package io.github.alkoleft.mcp.infrastructure.platform.dsl.designer.commands

/**
 * Трейт для поддержки параметра /UpdateDBCfg в командах конфигуратора
 *
 * Параметр /UpdateDBCfg допускается указывать после следующих параметров:
 * /LoadCfg — загрузка конфигурации из файла;
 * /UpdateCfg — обновление конфигурации, находящейся на поддержке;
 * /ConfigurationRepositoryUpdateCfg — обновление конфигурации из хранилища;
 * /LoadConfigFiles — загрузка файлов конфигурации;
 * /LoadConfigFromFiles — загрузка конфигурации из файлов;
 * /MobileAppUpdatePublication — обновление публикации мобильного приложения;
 * /MobileAppWriteFile — запись xml-файла мобильного приложения;
 * /MobileClientWriteFile — запись xml-файла мобильного клиента;
 * /MobileClientDigiSign — подпись мобильного клиента.
 */
interface UpdateDBCfgSupport {
    /**
     * Флаг для добавления параметра /UpdateDBCfg
     */
    var updateDBCfg: Boolean

    /**
     * DSL метод для включения параметра /UpdateDBCfg
     */
    fun updateDBCfg() {
        this.updateDBCfg = true
    }

    /**
     * Добавляет параметр /UpdateDBCfg к списку аргументов, если он включен
     */
    fun addUpdateDBCfgToArguments(args: MutableList<String>) {
        if (updateDBCfg) {
            args.add("/UpdateDBCfg")
        }
    }

    /**
     * Добавляет параметр updateDBCfg к параметрам команды, если он включен
     */
    fun addUpdateDBCfgToParameters(params: MutableMap<String, String>) {
        if (updateDBCfg) {
            params["updateDBCfg"] = "true"
        }
    }
}
