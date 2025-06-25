package io.github.lexadiky.ktor.openapivalidator

import com.atlassian.oai.validator.whitelist.rule.WhitelistRule
import io.github.lexadiky.ktor.openapivalidator.RuleMatcher.Operation
import io.github.lexadiky.ktor.openapivalidator.RuleMatcher.Request
import io.github.lexadiky.ktor.openapivalidator.RuleMatcher.Response
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

fun interface RuleMatcher {

    fun match(operation: Operation, request: Request, response: Response): Boolean

    class Request(val method: HttpMethod?)

    class Response(val code: HttpStatusCode?)

    class Operation(val id: String?)

    companion object {

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
        val agRequest = Request(method = request?.method?.name?.let(HttpMethod::parse))
        val agResponse = Response(code = response?.status?.let(HttpStatusCode::fromValue))

        this.match(agOperation, agRequest, agResponse)
    }
}

data class RuleMatchContext(
    val operation: Operation,
    val request: Request,
    val response: Response,
)
