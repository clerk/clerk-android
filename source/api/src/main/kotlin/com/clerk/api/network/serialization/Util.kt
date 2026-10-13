package com.clerk.api.network.serialization

internal fun <K, V> Map<K, V>.toUnmodifiableMap() = buildMap { putAll(this@toUnmodifiableMap) }
