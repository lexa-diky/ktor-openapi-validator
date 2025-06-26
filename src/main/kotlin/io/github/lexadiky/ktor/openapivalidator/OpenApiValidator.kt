@file:OptIn(OpenApiValidatorDelicateApi::class)

package io.github.lexadiky.ktor.openapivalidator

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist
import io.github.lexadiky.ktor.openapivalidator.reporter.ErrorReporter
import io.github.lexadiky.ktor.openapivalidator.reporter.Junit5ErrorReporter
import io.github.lexadiky.ktor.openapivalidator.reporter.reportIfErrors
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
import kotlin.reflect.KProperty

/**
 * Ktor HTTP client plugin for validating requests and responses against an OpenAPI specification.
 * Intended to be used only in tests.
 *
 * ### Usage:
 * ```kotlin
 * val client = HttpClient {
 *     // ... rest of your configuration
 *
 *     install(OpenApiValidator) {
 *         specificationUrl = "openapi.yaml" // this is required setting
 *     }
 * }
 * ```
 */
@Suppress("unused")
val OpenApiValidator = createClientPlugin(
    name = "OpenApiValidator", createConfiguration = ::OpenApiValidatorConfig
) {
    pluginConfig.prebuild()

    val validator = pluginConfig.validatorBuilder.build()
    val reporter = pluginConfig.reporter

    val requestContentAttr = AttributeKey<OutgoingContent>("OpenApiValidatorRequestContent")

    client.requestPipeline.intercept(HttpRequestPipeline.Transform) {
        val textContent = when (subject) {
            is OutgoingContent -> subject as OutgoingContent
            is String -> TextContent(
                subject as String, contentType = ContentType.Text.Any
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
        context.url.parameters.names().forEach { parameterName ->
            builder.withQueryParam(parameterName, context.url.parameters.getAll(parameterName) ?: emptyList())
        }
        val report = validator.validateRequest(builder.build())
        reporter.reportIfErrors(report)
    }

    client.receivePipeline.intercept(HttpReceivePipeline.Before) { response ->
        val saved = response.call.save()
        val body = saved.body<String>()
        val builder = SimpleResponse.Builder(response.status.value)

        if (body.isNotEmpty()) {
            builder.withBody(body)
        }

        val report = validator.validateResponse(
            response.request.url.encodedPath, Request.Method.valueOf(response.request.method.value), builder.build()
        )

        reporter.reportIfErrors(report)

        proceedWith(saved.response)
    }
}

class OpenApiValidatorConfig {
    private var mode = Mode.UNKNOWN
    internal var validatorBuilder = OpenApiInteractionValidator.Builder()
    internal var whitelist = ValidationErrorsWhitelist.create()

    /**
     * The error reporter used to report validation errors.
     * Defaults to [Junit5ErrorReporter].
     */
    var reporter: ErrorReporter = Junit5ErrorReporter()

    /**
     * The path or URL to the OpenAPI specification file.
     * Is required for the validator to function.
     */
    var specificationUrl: String? by AgnosticParam {
        withApiSpecificationUrl(it)
    }

    /**
     * Adds a whitelist rule to ignore specific requests or responses during validation.
     *
     * @param name the name of the whitelist rule for identification
     * @param block a predicate that receives a [RuleMatchContext] and returns `true` if the rule should match.
     * @throws IllegalArgumentException if the configuration mode is set to Atlassian-specific
     */
    fun whitelist(name: String, block: RuleMatchContext.() -> Boolean) {
        require(mode != Mode.ATLASSIAN, CONFIG_TYPE_MESSAGE)
        mode = Mode.AGNOSTIC
        whitelist = whitelist.withRule(name, RuleMatcher.from(block).intoAtlassianWhitelistRule())
    }

    /**
     * Allows configuring the underlying Atlassian OpenApiInteractionValidator directly.
     *
     * Use this method if you need to access advanced or implementation-specific features
     * of the Atlassian validator. This method is marked with [OpenApiValidatorDelicateApi]
     * and is mutually exclusive with agnostic configuration methods.
     *
     * @param block configuration block for OpenApiInteractionValidator.Builder
     * @throws IllegalArgumentException if agnostic configuration was already used
     */
    @OpenApiValidatorDelicateApi
    fun atlassian(block: OpenApiInteractionValidator.Builder.() -> Unit) {
        require(mode != Mode.AGNOSTIC, CONFIG_TYPE_MESSAGE)

        mode = Mode.ATLASSIAN
        validatorBuilder.apply(block)
    }

    internal fun prebuild() {
        validatorBuilder = validatorBuilder.withWhitelist(whitelist)
    }

    internal enum class Mode {
        UNKNOWN, AGNOSTIC, ATLASSIAN
    }

    private class AgnosticParam<T>(private val bloc: OpenApiInteractionValidator.Builder.(T) -> Unit) {
        private var field: T? = null

        operator fun getValue(thisRef: OpenApiValidatorConfig, property: KProperty<*>): T? {
            return field
        }

        operator fun setValue(thisRef: OpenApiValidatorConfig, property: KProperty<*>, value: T) {
            require(thisRef.mode != Mode.ATLASSIAN, CONFIG_TYPE_MESSAGE)
            thisRef.mode = Mode.AGNOSTIC
            bloc.invoke(thisRef.validatorBuilder, value)
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
