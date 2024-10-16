package com.gs.wialonlocal.common

expect fun openNavigationApp(endLatitude: Double, endLongitude: Double, context: Any?)

// Shared module: Common.kt
interface UrlSharer {
    fun shareUrl(url: String, context: Any?)
}

expect fun getUrlSharer(): UrlSharer

expect fun getDevice(): String
expect fun getVersion(): String
expect fun getStoreUrl(): String

