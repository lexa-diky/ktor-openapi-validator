package io.github.lexadiky.ktor.openapivalidator.reporter

import com.atlassian.oai.validator.report.ValidationReport
import io.github.lexadiky.ktor.openapivalidator.OpenApiValidatorDelicateApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertAll

@OptIn(OpenApiValidatorDelicateApi::class)
class Junit5ErrorReporter : ErrorReporter {

    override fun report(validationReport: ValidationReport) {
        assertAll(
            heading = "Openapi validation report",
            executables = validationReport.messages.map { it.toJunitAssertionExecutable() }
                .toTypedArray()
        )
    }

    private fun ValidationReport.Message.toJunitAssertionExecutable(): () -> Unit = {
        Assertions.fail(message.toString())
    }
}
