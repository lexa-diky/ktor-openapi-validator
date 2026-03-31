package io.github.lexadiky.ktor.openapivalidator.cache

import io.ktor.util.collections.ConcurrentMap

internal class MemCache<V> {

    private val cache = ConcurrentMap<String, V>()

    fun get(key: String): V? = cache[key]

    fun compute(key: String, fn: (String) -> V): V = cache.computeIfAbsent(
        key,
        fn
    )
}