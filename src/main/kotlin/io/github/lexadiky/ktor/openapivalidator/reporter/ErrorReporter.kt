package io.github.lexadiky.ktor.openapivalidator.reporter

import com.atlassian.oai.validator.report.ValidationReport
import io.github.lexadiky.ktor.openapivalidator.OpenApiValidatorDelicateApi

interface ErrorReporter {

    @OpenApiValidatorDelicateApi
    fun report(validationReport: ValidationReport)
}

@OptIn(OpenApiValidatorDelicateApi::class)
internal fun ErrorReporter.reportIfErrors(validationReport: ValidationReport) {
    if (validationReport.hasErrors()) {
        this.report(validationReport)
    }
}
