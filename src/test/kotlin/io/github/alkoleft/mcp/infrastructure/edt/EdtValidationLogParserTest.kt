package io.github.alkoleft.mcp.infrastructure.edt

import io.github.alkoleft.mcp.application.services.validation.EdtIssueLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EdtValidationLogParserTest {
    private val parser: EdtValidationLogParser = EdtValidationLogParser()

    @Test
    fun `parse should read issues from tsv`() {
        val logContent: String =
            """
            2025-11-05T02:23:47+0300	Незначительная	Предупреждение	yaxunit	com._1c.g5.v8.dt.bsl:bsl-legacy-check-string-literal	ОбщийМодуль.ЮТТипыДанныхСлужебный.Модуль	строка 182	Возможно, строковый литерал содержит ошибку [Сервер, Тонкий клиент, Толстый клиент (управляемое приложение), Web-клиент, Толстый клиент (обычное приложение)]
            2025-10-15T22:39:13+0300	Незначительная	Предупреждение	yaxunit	com._1c.g5.v8.dt.form:form-legacy-check-command-handler	Обработка.ЮТПомощникДляСозданияТестовыхДанных.Форма.Форма.Форма	СнятьВсеФлажки	Для команды формы "СнятьВсеФлажки" назначен обработчик "СнятьВсеФлажки", но метод с таким именем отсутствует в модуле формы
            """.trimIndent()
        val analysis = parser.parse(logContent)

        val firstIssue = analysis[0]
        assertThat(firstIssue.level).isEqualTo(EdtIssueLevel.MINOR)
        assertThat(firstIssue.message).startsWith("Возможно, строковый литерал содержит ошибку")
        assertThat(firstIssue.path).isEqualTo("ОбщийМодуль.ЮТТипыДанныхСлужебный.Модуль")
        assertThat(firstIssue.place).isEqualTo("строка 182")
        assertThat(firstIssue.issueId).isEqualTo("bsl-legacy-check-string-literal")
        assertThat(firstIssue.project).isEqualTo("yaxunit")

        val secondIssue = analysis[1]
        assertThat(secondIssue.level).isEqualTo(EdtIssueLevel.MINOR)
        assertThat(secondIssue.message)
            .isEqualTo(
                "Для команды формы \"СнятьВсеФлажки\" назначен обработчик \"СнятьВсеФлажки\", но метод с таким именем отсутствует в модуле формы",
            )
        assertThat(secondIssue.path).isEqualTo("Обработка.ЮТПомощникДляСозданияТестовыхДанных.Форма.Форма.Форма")
        assertThat(secondIssue.place).isEqualTo("СнятьВсеФлажки")
        assertThat(secondIssue.issueId).isEqualTo("form-legacy-check-command-handler")
        assertThat(secondIssue.project).isEqualTo("yaxunit")
    }

    @Test
    fun `parse should skip malformed lines`() {
        val logContent: String =
            """
            2025-11-05T02:23:47+0300	Незначительная	Предупреждение	yaxunit	bsl-legacy-check-string-literal
            2025-11-05T02:23:51+0300	Незначительная	Предупреждение	yaxunit	bsl-legacy-check-string-literal	ОбщийМодуль.ЮТРеактивныйКлиент.Модуль	строка 52	Возможно, строковый литерал содержит ошибку
            """.trimIndent()
        val analysis = parser.parse(logContent)
        val issue = analysis.first()
        assertThat(issue.path).isEqualTo("ОбщийМодуль.ЮТРеактивныйКлиент.Модуль")
    }
}
