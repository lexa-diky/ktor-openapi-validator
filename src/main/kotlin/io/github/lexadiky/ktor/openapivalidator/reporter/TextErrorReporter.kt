package io.github.lexadiky.ktor.openapivalidator.reporter

import com.atlassian.oai.validator.report.ValidationReport
import io.github.lexadiky.ktor.openapivalidator.OpenApiValidatorDelicateApi

/**
 * Abstract [ErrorReporter] that reports validation errors as a list of text messages.
 *
 * Subclasses must implement [report] to handle the list of error messages.
 * The [report] method transforms the [ValidationReport] into a list of strings and delegates to [report].
 */
@OptIn(OpenApiValidatorDelicateApi::class)
abstract class TextErrorReporter : ErrorReporter {

    final override fun report(validationReport: ValidationReport) {
        report(validationReport.messages.map { it.toString() })
    }

    abstract fun report(message: List<String>)

    companion object {

        operator fun invoke(fn: (List<String>) -> Unit): ErrorReporter {
            return object : TextErrorReporter() {
                override fun report(message: List<String>) {
                    fn(message)
                }
            }
        }
    }
}
