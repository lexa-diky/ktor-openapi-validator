package io.github.lexadiky.ktor.openapivalidator.reporter

import com.atlassian.oai.validator.report.ValidationReport
import io.github.lexadiky.ktor.openapivalidator.OpenApiValidatorDelicateApi

/**
 * Implementation of [ErrorReporter] that throws an [AssertionError] when the provided [ValidationReport]
 * contains any validation errors.
 */
class AssertionErrorThrowReporter : ErrorReporter {

    @OpenApiValidatorDelicateApi
    override fun report(validationReport: ValidationReport) {
        if (validationReport.hasErrors()) {
            throw AssertionError(
                validationReport.messages.joinToString(separator = "\n") { it.toString() }
            )
        }
    }
}
