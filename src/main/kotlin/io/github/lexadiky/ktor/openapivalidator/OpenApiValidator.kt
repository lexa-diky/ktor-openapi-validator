package io.github.lexadiky.ktor.openapivalidator

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.ValidationReport
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist
import com.atlassian.oai.validator.whitelist.rule.WhitelistRule
import io.ktor.client.call.*
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.TextContent
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import org.junit.jupiter.api.Assertions
import kotlin.reflect.KProperty

/**
 * Ktor HTTP client plugin for validating requests and responses against an OpenAPI specification.
 * Intended to be used only in tests.
 */
@Suppress("unused")
val OpenApiValidator = createClientPlugin(
    name = "OpenApiValidator",
    createConfiguration = ::OpenApiValidatorConfig
) {
    val validator = pluginConfig.prebuild()
        .build()

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

class OpenApiValidatorConfig {
    private var mode = Mode.UNKNOWN
    private val builder = OpenApiInteractionValidator.Builder()
    internal val whitelist = ValidationErrorsWhitelist.create()

    var specificationUrl: String? by AgnosticParam {
        withApiSpecificationUrl(it)
    }

    fun whitelist(name: String, block: RuleMatchContext.() -> Boolean) {
        whitelist.withRule(name, RuleMatcher.from(block).intoAtlassianWhitelistRule())
    }

    @OpenApiValidatorDelicateApi
    fun atlassian(block: OpenApiInteractionValidator.Builder.() -> Unit) {
        require(mode != Mode.AGNOSTIC, CONFIG_TYPE_MESSAGE)

        mode = Mode.ATLASSIAN
        builder.apply(block)
    }

    internal fun prebuild(): OpenApiInteractionValidator.Builder =
        builder.withWhitelist(whitelist)

    internal enum class Mode {
        UNKNOWN, AGNOSTIC, ATLASSIAN
    }

    private class AgnosticParam<T>(private val bloc: OpenApiInteractionValidator.Builder.(T) -> Unit) {
        private var field: T? = null

        operator fun getValue(thisRef: OpenApiValidatorConfig, property: KProperty<*>): T? {
            return field
        }

        operator fun setValue(thisRef: OpenApiValidatorConfig, property: KProperty<*>, value: T?) {
            require(thisRef.mode != Mode.ATLASSIAN, CONFIG_TYPE_MESSAGE)
            thisRef.mode = Mode.AGNOSTIC
            field = value
        }
    }

    companion object {

        val CONFIG_TYPE_MESSAGE: () -> Any = {
            """You configured validator with both implementation agnostic and atlassian validator specific parameters. 
                |This is not allowed, please use either agnostic methods not marked with @OpenApiValidatorDelicateApi. 
                |Or remove them in favor of `atlassian { ... }` configuration marked with @OpenApiValidatorDelicateApi.
                |""".trimMargin()
        }
    }
}

private fun assertReportJunit5(validationReport: ValidationReport) {
    if (validationReport.hasErrors()) {
        validationReport.messages.forEach { message ->
            Assertions.fail(message.toString())
        }
    }
}
