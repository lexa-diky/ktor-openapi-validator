package io.github.lexadiky.ktor.openapivalidator.cache

import io.swagger.v3.oas.models.OpenAPI

internal object StaticCacheHolder {
    internal val atlassianSpecifications = MemCache<OpenAPI>()
}