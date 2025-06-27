package io.github.lexadiky.ktor.openapivalidator

import com.atlassian.oai.validator.whitelist.rule.WhitelistRule
import io.github.lexadiky.ktor.openapivalidator.RuleMatcher.Operation
import io.github.lexadiky.ktor.openapivalidator.RuleMatcher.Request
import io.github.lexadiky.ktor.openapivalidator.RuleMatcher.Response
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import io.ktor.http.parameters
import io.ktor.http.parametersOf
import kotlin.jvm.optionals.getOrNull

/**
 * Interface for matching OpenAPI operations, requests, and responses against custom rules.
 *
 * Implementations should return `true` if the given operation, request, and response match the rule criteria.
 */
fun interface RuleMatcher {

    fun match(operation: Operation, request: Request, response: Response): Boolean

    class Request(
        val method: HttpMethod?,
        val path: String?,
        val headers: Headers,
        val parameters: Parameters,
        val body: String?,
    )

    class Response(val code: HttpStatusCode?, val body: String?)

    class Operation(val id: String?)

    companion object {

        /**
         * Creates a `RuleMatcher` from a lambda, enabling a DSL-like style for defining matching rules.
         */
        fun from(block: RuleMatchContext.() -> Boolean): RuleMatcher {
            return object : RuleMatcher {
                override fun match(
                    operation: Operation,
                    request: Request,
                    response: Response
                ): Boolean {
                    val context = RuleMatchContext(operation, request, response)
                    return context.block()
                }
            }
        }
    }
}

internal fun RuleMatcher.intoAtlassianWhitelistRule(): WhitelistRule {
    return WhitelistRule { message, operation, request, response ->
        val agOperation = Operation(id = operation?.operation?.operationId)
        val agRequest = Request(
            method = request?.method?.name?.let(HttpMethod::parse),
            path = request?.path,
            headers = headersOf(
                pairs = request?.headers
                    ?.map { (name, values) -> name to values.toList() }?.toTypedArray()
                    ?: emptyArray()
            ),
            parameters = parametersOf(
                map = request?.queryParameters
                    ?.associate { name -> name to request.getQueryParameterValues(name).toList() }
                    ?: emptyMap()
            ),
            body = request?.requestBody?.getOrNull()?.toString(Charsets.UTF_8),
        )
        val agResponse = Response(
            code = response?.status?.let(HttpStatusCode::fromValue),
            body = response?.responseBody?.getOrNull()?.toString(Charsets.UTF_8),
        )
        this.match(agOperation, agRequest, agResponse)
    }
}

data class RuleMatchContext(
    val operation: Operation,
    val request: Request,
    val response: Response,
)
