package io.github.alkoleft.mcp.infrastructure.designer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DesignerValidationLogParserTest {
    private val parser: DesignerValidationLogParser = DesignerValidationLogParser()

    @Test
    fun `parse should categorize module metadata and extension issues`() {
        // Подготовка: комбинированный лог с модульной ошибкой, объектом конфигурации и расширением
        val logContent: String =
            """
            {ОбщийМодуль.Интеграция.Модуль(180,13)}: Процедура или функция с указанным именем не определена (ПрочитатьJSON)
            	Значение = <<?>>ПрочитатьJSON(Чтение); (Проверка: Веб-клиент)
            Справочник.ХранимыеФайлы.Форма.ФормаЭлемента.Форма ошибочное свойство: "Аудио"
            tests ОбщийМодуль.ОМ_ЮТУтверждения.Модуль Не обнаружено ссылок на процедуру: "ПроверитьДанныеОшибкиУтверждения"
            """.trimIndent()
        // Действие: парсинг логов
        val analysis: ConfiguratorLogAnalysis = parser.parse(logContent)
        // Проверка: общее количество и агрегированные счётчики
        assertThat(analysis.entries).hasSize(3)
        // Проверка модульной ошибки
        val moduleIssue: ConfiguratorIssue = analysis.entries[0]
        assertThat(moduleIssue).isInstanceOf(ConfiguratorIssue.ModuleIssue::class.java)

        val metadataIssue: ConfiguratorIssue = analysis.entries[1]
        assertThat(metadataIssue).isInstanceOf(ConfiguratorIssue.ObjectIssue::class.java)

        val objectIssue: ConfiguratorIssue = analysis.entries[1]
        assertThat(objectIssue).isInstanceOf(ConfiguratorIssue.ObjectIssue::class.java)

        val extensionIssue: ConfiguratorIssue = analysis.entries[2]
        assertThat(extensionIssue).isInstanceOf(ConfiguratorIssue.ObjectIssue::class.java)

        val moduleDetails: ConfiguratorIssue.ModuleIssue = moduleIssue as ConfiguratorIssue.ModuleIssue
        assertThat(moduleDetails.path).isEqualTo("ОбщийМодуль.Интеграция.Модуль")
        assertThat(moduleDetails.extension).isNull()
        assertThat(moduleDetails.line).isEqualTo(180)
        assertThat(moduleDetails.column).isEqualTo(13)
        assertThat(moduleDetails.description).isEqualTo("Процедура или функция с указанным именем не определена (ПрочитатьJSON)")
        assertThat(moduleDetails.context).isEqualTo("Веб-клиент")
        // Проверка ошибки объекта конфигурации
        val metadataDetails: ConfiguratorIssue.ObjectIssue = objectIssue as ConfiguratorIssue.ObjectIssue
        assertThat(metadataDetails.message).isEqualTo("ошибочное свойство: \"Аудио\"")
        // Проверка ошибки расширения
        val extensionDetails: ConfiguratorIssue.ObjectIssue = extensionIssue as ConfiguratorIssue.ObjectIssue
        assertThat(extensionDetails.extension).isEqualTo("tests")
        assertThat(extensionDetails.message).isEqualTo("Не обнаружено ссылок на процедуру: \"ПроверитьДанныеОшибкиУтверждения\"")
    }

    @Test
    fun `parse should extract details for handler absence`() {
        // Подготовка: пример строки расширения с отсутствующим обработчиком
        val logContent: String =
            """
            YAXUNIT Обработка.ЮТПомощникДляСозданияТестовыхДанных.Форма.Форма.Форма Отсутствует обработчик:  СнятьВсеФлажки "СнятьВсеФлажки"
            """.trimIndent()
        // Действие: парсинг одной ошибки
        val analysis: ConfiguratorLogAnalysis = parser.parse(logContent)
        // Проверка: результат содержит одну запись с корректно извлечёнными полями
        assertThat(analysis.entries).hasSize(1)
        val entry: ConfiguratorIssue.ObjectIssue = analysis.entries[0] as ConfiguratorIssue.ObjectIssue
        assertThat(entry.extension).isEqualTo("YAXUNIT")
        assertThat(entry.message).isEqualTo("Отсутствует обработчик:  СнятьВсеФлажки \"СнятьВсеФлажки\"")
    }

    @Test
    fun `parse should support module errors with extension prefix`() {
        // Подготовка: модульная ошибка с префиксом расширения внутри скобок
        val logContent: String =
            """
            {tests ОбщийМодуль.ОМ_ВыполнениеТестовОбработки.Модуль(54,2)}: Переменная не определена (ЮТест)
            	<<?>>ЮТест.ОжидаетЧто(ИсполняемыеСценарии) (Проверка: Мобильное приложение-сервер)
            """.trimIndent()
        // Действие: парсинг лога
        val analysis: ConfiguratorLogAnalysis = parser.parse(logContent)
        // Проверка: модуль распознан, контекст сохранён
        assertThat(analysis.entries).hasSize(1)
        val moduleIssue: ConfiguratorIssue.ModuleIssue = analysis.entries[0] as ConfiguratorIssue.ModuleIssue
        assertThat(moduleIssue.path).isEqualTo("ОбщийМодуль.ОМ_ВыполнениеТестовОбработки.Модуль")
        assertThat(moduleIssue.extension).isEqualTo("tests")
        assertThat(moduleIssue.line).isEqualTo(54)
        assertThat(moduleIssue.column).isEqualTo(2)
        assertThat(moduleIssue.description).isEqualTo("Переменная не определена (ЮТест)")
        assertThat(moduleIssue.context).isEqualTo("Мобильное приложение-сервер")
    }
}
