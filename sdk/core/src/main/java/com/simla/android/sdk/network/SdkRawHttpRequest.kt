package com.simla.android.sdk.network

data class SdkRawHttpRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray,
    val contentType: String = "application/octet-stream",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SdkRawHttpRequest

        if (url != other.url) return false
        if (headers != other.headers) return false
        if (!body.contentEquals(other.body)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}
