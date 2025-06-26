package io.github.lexadiky.ktor.openapivalidator.reporter

import com.atlassian.oai.validator.report.ValidationReport
import io.github.lexadiky.ktor.openapivalidator.OpenApiValidatorDelicateApi

@OptIn(OpenApiValidatorDelicateApi::class)
abstract class TextErrorReporter : ErrorReporter {

    override fun report(validationReport: ValidationReport) {
        reportMessages(validationReport.messages.map { it.toString() })
    }

    abstract fun reportMessages(message: List<String>)

    companion object {

        operator fun invoke(fn: (List<String>) -> Unit): ErrorReporter {
            return object : TextErrorReporter() {
                override fun reportMessages(message: List<String>) {
                    fn(message)
                }
            }
        }
    }
}
