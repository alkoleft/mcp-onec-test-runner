package io.github.alkoleft.mcp.infrastructure.designer

import io.github.alkoleft.mcp.application.services.validation.Issue
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
        val analysis = parser.parse(logContent)
        // Проверка: общее количество и агрегированные счётчики
        assertThat(analysis).hasSize(3)
        // Проверка модульной ошибки
        val moduleIssue = analysis[0]
        assertThat(moduleIssue).isInstanceOf(Issue.ModuleIssue::class.java)

        val metadataIssue = analysis[1]
        assertThat(metadataIssue).isInstanceOf(Issue.ObjectIssue::class.java)

        val objectIssue = analysis[1]
        assertThat(objectIssue).isInstanceOf(Issue.ObjectIssue::class.java)

        val extensionIssue = analysis[2]
        assertThat(extensionIssue).isInstanceOf(Issue.ObjectIssue::class.java)

        val moduleDetails = moduleIssue as Issue.ModuleIssue
        assertThat(moduleDetails.path).isEqualTo("ОбщийМодуль.Интеграция.Модуль")
        assertThat(moduleDetails.extension).isNull()
        assertThat(moduleDetails.line).isEqualTo(180)
        assertThat(moduleDetails.column).isEqualTo(13)
        assertThat(moduleDetails.message).isEqualTo("Процедура или функция с указанным именем не определена (ПрочитатьJSON)")
        assertThat(moduleDetails.context).isEqualTo("Веб-клиент")
        // Проверка ошибки объекта конфигурации
        val metadataDetails = objectIssue as Issue.ObjectIssue
        assertThat(metadataDetails.message).isEqualTo("ошибочное свойство: \"Аудио\"")
        // Проверка ошибки расширения
        val extensionDetails = extensionIssue as Issue.ObjectIssue
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
        val analysis = parser.parse(logContent)
        // Проверка: результат содержит одну запись с корректно извлечёнными полями
        assertThat(analysis).hasSize(1)
        val entry = analysis[0] as Issue.ObjectIssue
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
        val analysis = parser.parse(logContent)
        // Проверка: модуль распознан, контекст сохранён
        assertThat(analysis).hasSize(1)
        val moduleIssue = analysis[0] as Issue.ModuleIssue
        assertThat(moduleIssue.path).isEqualTo("ОбщийМодуль.ОМ_ВыполнениеТестовОбработки.Модуль")
        assertThat(moduleIssue.extension).isEqualTo("tests")
        assertThat(moduleIssue.line).isEqualTo(54)
        assertThat(moduleIssue.column).isEqualTo(2)
        assertThat(moduleIssue.message).isEqualTo("Переменная не определена (ЮТест)")
        assertThat(moduleIssue.context).isEqualTo("Мобильное приложение-сервер")
    }
}
