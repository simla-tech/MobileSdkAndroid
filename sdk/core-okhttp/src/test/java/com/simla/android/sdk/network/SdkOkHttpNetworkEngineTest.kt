package com.simla.android.sdk.network

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.TimeUnit

class SdkOkHttpNetworkEngineTest {
    @Test
    fun `created engine uses default timeouts`() {
        val engine = SdkOkHttpNetworkEngine.create()

        assertEquals(30_000, engine.httpClient.connectTimeoutMillis)
        assertEquals(30_000, engine.httpClient.readTimeoutMillis)
        assertEquals(30_000, engine.httpClient.writeTimeoutMillis)
    }

    @Test
    fun `created engine applies timeout options`() {
        val engine = SdkOkHttpNetworkEngine.create(
            options = SdkOkHttpNetworkEngine.Options(
                connectTimeoutSeconds = 5,
                readTimeoutSeconds = 10,
                writeTimeoutSeconds = 15,
            ),
        )

        assertEquals(5_000, engine.httpClient.connectTimeoutMillis)
        assertEquals(10_000, engine.httpClient.readTimeoutMillis)
        assertEquals(15_000, engine.httpClient.writeTimeoutMillis)
    }

    @Test
    fun `external client settings are preserved`() {
        val client = OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(9, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .build()

        val engine = SdkOkHttpNetworkEngine.from(client)

        assertSame(client, engine.httpClient)
        assertEquals(7_000, engine.httpClient.connectTimeoutMillis)
        assertEquals(8_000, engine.httpClient.readTimeoutMillis)
        assertEquals(9_000, engine.httpClient.writeTimeoutMillis)
        assertEquals(10_000, engine.httpClient.pingIntervalMillis)
    }

    @Test
    fun `negative timeout options are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            SdkOkHttpNetworkEngine.Options(connectTimeoutSeconds = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SdkOkHttpNetworkEngine.Options(readTimeoutSeconds = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SdkOkHttpNetworkEngine.Options(writeTimeoutSeconds = -1)
        }
    }
}
