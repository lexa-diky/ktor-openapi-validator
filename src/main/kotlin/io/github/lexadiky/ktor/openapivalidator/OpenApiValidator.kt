package io.github.lexadiky.ktor.openapivalidator

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.ValidationReport
import io.ktor.client.call.body
import io.ktor.client.call.save
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.client.statement.request
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey
import org.junit.jupiter.api.Assertions

/**
 * Ktor HTTP client plugin for validating requests and responses against an OpenAPI specification.
 * Intended to be used only in tests.
 */
@Suppress("unused")
val OpenApiValidator = createClientPlugin(
    name = "OpenApiValidator",
    createConfiguration = ::OpenApiValidatorConfig
) {
    val builder = OpenApiInteractionValidator.Builder()

    require(
        (pluginConfig.specificationUrl != null) xor
                (pluginConfig.atlassianValidatorConfigFn != null)
    ) {
        "Plugin could be configured either by setting validator agnostic parameters like " +
                "`specificationUrl` or by `atlassian { ... }` not both."
    }

    if (pluginConfig.specificationUrl != null || pluginConfig.atlassianValidatorConfigFn != null) {
        "Please configure `specificationUrl` to specify which `openapi` specification should be used"
    }

    if (pluginConfig.atlassianValidatorConfigFn != null) {
        builder.apply(pluginConfig.atlassianValidatorConfigFn!!)
    }

    val validator = builder.build()

    val requestContentAttr = AttributeKey<OutgoingContent>("OpenApiValidatorRequestContent")

    client.requestPipeline.intercept(HttpRequestPipeline.Transform) {
        val textContent = when (subject) {
            is OutgoingContent -> subject as OutgoingContent
            is String -> TextContent(
                subject as String,
                contentType = ContentType.Text.Any
            )

            else -> error("Unsupported subject type: ${subject::class}")
        }
        this.context.attributes.put(requestContentAttr, textContent)
    }

    client.requestPipeline.intercept(HttpRequestPipeline.Send) {
        val requestContent = context.attributes[requestContentAttr]

        val builder = SimpleRequest.Builder(context.method.value, context.url.encodedPath)
        if (requestContent is TextContent) {
            builder.withBody(requestContent.text)
        }
        context.headers.entries().forEach {
            builder.withHeader(it.key, it.value)
        }
        val report = validator.validateRequest(builder.build())
        assertReportJunit5(report)
    }


    client.receivePipeline.intercept(HttpReceivePipeline.Before) { response ->
        val saved = response.call.save()
        val body = saved.body<String>()
        val builder = SimpleResponse.Builder(response.status.value)

        if (body.isNotEmpty()) {
            builder.withBody(body)
        }

        val report = validator.validateResponse(
            response.request.url.encodedPath,
            Request.Method.valueOf(response.request.method.value),
            builder.build()
        )

        assertReportJunit5(report)

        proceedWith(saved.response)
    }
}

data class OpenApiValidatorConfig(
    /**
     * Path to your `openapi` specification file.
     */
    var specificationUrl: String? = null,
    internal var atlassianValidatorConfigFn: (OpenApiInteractionValidator.Builder.() -> Unit)? = null,
) {

    @OpenApiValidatorDelicateApi
    fun atlassian(block: OpenApiInteractionValidator.Builder.() -> Unit) {
        atlassianValidatorConfigFn = block
    }
}

private fun assertReportJunit5(validationReport: ValidationReport) {
    if (validationReport.hasErrors()) {
        validationReport.messages.forEach { message ->
            Assertions.fail(message.toString())
        }
    }
}
