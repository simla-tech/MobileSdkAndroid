package com.simla.android.sdk.tracker

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TrackerEventDataFactoryTest {
    @Test
    fun cart_items_use_backend_item_identifier_keys() {
        val data = TrackerEventDataFactory.cart(
            cartId = "cart-42",
            items = listOf(
                TrackerCartItem(TrackerOfferIdentifier.SystemId(1), price = 10.0, quantity = 1),
                TrackerCartItem(
                    TrackerOfferIdentifier.ExternalId("external-2"),
                    price = 20.0,
                    quantity = 2,
                ),
                TrackerCartItem(
                    TrackerOfferIdentifier.XmlId("xml-3"),
                    price = 30.0,
                    quantity = 3,
                ),
            ),
        )

        val items = data.getValue("items").jsonArray.map { it.jsonObject }
        assertEquals("1", items[0].getValue("id").jsonPrimitive.content)
        assertEquals("external-2", items[1].getValue("external_id").jsonPrimitive.content)
        assertEquals("xml-3", items[2].getValue("xml_id").jsonPrimitive.content)

        items.forEach { item ->
            assertFalse("offer_id" in item)
            assertFalse("offer_external_id" in item)
            assertFalse("offer_xml_id" in item)
        }
    }

    @Test
    fun top_level_offer_identifier_keys_are_unchanged() {
        val data = TrackerEventDataFactory.pageView(
            TrackerViewedProduct(offer = TrackerOfferIdentifier.ExternalId("external-42")),
        )

        requireNotNull(data)
        assertEquals("external-42", data.getValue("offer_external_id").jsonPrimitive.content)
        assertFalse("external_id" in data)
    }
}
