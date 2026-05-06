package com.mramfix.subtracker.image

import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.memory.MemoryCache

fun subscriptionIconCacheKey(id: Long, iconUri: String): String = "subscription-icon-$id-${iconUri.trim()}"

@OptIn(ExperimentalCoilApi::class)
fun ImageLoader.removeSubscriptionIconFromCache(id: Long, iconUri: String?) {
    val cleanUri = iconUri?.trim().orEmpty()
    if (cleanUri.isBlank()) return
    val key = subscriptionIconCacheKey(id, cleanUri)
    memoryCache?.remove(MemoryCache.Key(key))
    diskCache?.remove(key)
}
