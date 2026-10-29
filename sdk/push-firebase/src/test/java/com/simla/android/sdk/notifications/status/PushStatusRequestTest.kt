package com.simla.android.sdk.notifications.status

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PushStatusRequestTest {
    @Test
    fun `serializes push service status fields`() {
        val body = Json.encodeToString(
            PushStatusRequest(
                pushId = "app_42",
                status = PushStatus.DELIVERED.apiValue,
                changedAt = 1_786_118_400,
            ),
        )

        val json = Json.parseToJsonElement(body).jsonObject
        assertEquals("app_42", json.getValue("push_id").jsonPrimitive.content)
        assertEquals("delivered", json.getValue("status").jsonPrimitive.content)
        assertEquals(1_786_118_400L, json.getValue("changed_at").jsonPrimitive.content.toLong())
        assertFalse("button_hash" in json)
    }

    @Test
    fun `serializes button hash for button click`() {
        val body = PushStatusJson.encodeToString(
            PushStatusRequest(
                pushId = "app_42",
                status = PushStatus.CLICKED.apiValue,
                changedAt = 1_786_118_400,
                buttonHash = "abc123",
            ),
        )

        val json = Json.parseToJsonElement(body).jsonObject
        assertEquals("abc123", json.getValue("button_hash").jsonPrimitive.content)
    }
}
