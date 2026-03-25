package io.github.lexadiky.ktor.openapivalidator.reporter

import com.atlassian.oai.validator.report.ValidationReport
import io.github.lexadiky.ktor.openapivalidator.OpenApiValidatorDelicateApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.function.Executable

@OptIn(OpenApiValidatorDelicateApi::class)
class Junit5ErrorReporter : ErrorReporter {

    override fun report(validationReport: ValidationReport) {
        Assertions.assertAll(
            "Openapi validation report",
            validationReport.messages.map { it.toJunitAssertionExecutable() }
        )
    }

    private fun ValidationReport.Message.toJunitAssertionExecutable(): Executable = Executable {
        Assertions.fail(message.toString())
    }
}
