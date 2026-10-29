package com.simla.android.sdk.consultant.models

import com.simla.android.sdk.consultant.models.message.ConsultantMessage
import com.simla.android.sdk.consultant.serialization.SdkJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConsultantMessageDecodeTest {
    @Test
    fun image_message_decodes_backend_items_and_quote_content() {
        val message = SdkJson.decodeFromString<ConsultantMessage>(
            """
            {
              "id": "101",
              "type": "image",
              "content": "Посмотрите фото",
              "quote_content": "Фото",
              "quote_message_id": 100,
              "items": [
                {
                  "id": "file-1",
                  "caption": "photo.png",
                  "size": 2048,
                  "width": 640,
                  "height": 480,
                  "url": "https://example.test/photo.png"
                }
              ]
            }
            """.trimIndent(),
        )

        val item = message.items.first()

        assertEquals("101", message.idValue)
        assertEquals("image", message.type)
        assertEquals("Фото", message.quoteContent)
        assertEquals("100", message.quoteMessageIdValue)
        assertEquals("file-1", item.idValue)
        assertEquals("photo.png", item.displayFileName)
        assertEquals("https://example.test/photo.png", item.imageUrl)
        assertEquals(640, item.width)
        assertEquals(480, item.height)
    }

    @Test
    fun file_message_without_url_keeps_id_for_later_file_link_request() {
        val message = SdkJson.decodeFromString<ConsultantMessage>(
            """
            {
              "id": "102",
              "type": "file",
              "content": "",
              "items": [
                {
                  "id": "file-2",
                  "caption": "presentation.pdf",
                  "size": 123456
                }
              ]
            }
            """.trimIndent(),
        )

        val item = message.items.first()

        assertEquals("file-2", item.idValue)
        assertEquals("presentation.pdf", item.displayFileName)
        assertEquals(123456L, item.fileSizeBytes)
        assertNull(item.fileUrl)
    }

    @Test
    fun message_decodes_auto_message_attachments() {
        val message = SdkJson.decodeFromString<ConsultantMessage>(
            """
            {
              "id": "105",
              "type": "text",
              "content": "Choose an option",
              "attachments": {
                "links": [
                  {
                    "type": "link",
                    "label": "Open catalog",
                    "url": "https://example.test/catalog"
                  }
                ],
                "suggestions": [
                  {
                    "type": "text",
                    "title": "Call me back"
                  }
                ]
              }
            }
            """.trimIndent(),
        )

        val link = message.attachments?.links?.first()
        val suggestion = message.attachments?.suggestions?.first()

        assertEquals("105", message.idValue)
        assertEquals("Open catalog", link?.label)
        assertEquals("https://example.test/catalog", link?.url)
        assertEquals("Call me back", suggestion?.title)
    }

    @Test
    fun product_message_decodes_backend_product_payload() {
        val message = SdkJson.decodeFromString<ConsultantMessage>(
            """
            {
              "id": "103",
              "type": "product",
              "content": "",
                "product": {
                  "id": 77,
                  "name": "Chocolate candies",
                  "article": "SKU-77",
                  "url": "https://example.test/products/77",
                  "img": "https://example.test/products/77.png",
                  "unit": "pcs",
                  "cost": {
                    "value": 12.9,
                    "currency": "USD"
                  }
                }
              }
            """.trimIndent(),
        )

        val product = message.product

        assertEquals("103", message.idValue)
        assertEquals("product", message.type)
        assertEquals("77", product?.idValue)
        assertEquals("Chocolate candies", product?.name)
        assertEquals("SKU-77", product?.article)
        assertEquals("https://example.test/products/77", product?.url)
        assertEquals("https://example.test/products/77.png", product?.img)
        assertEquals("pcs", product?.unit)
        assertEquals(12.9, product?.cost?.value ?: 0.0, 0.0)
        assertEquals("USD", product?.cost?.currency)
    }

    @Test
    fun order_message_decodes_backend_order_payload() {
        val message = SdkJson.decodeFromString<ConsultantMessage>(
            """
            {
              "id": "104",
              "type": "order",
              "content": "",
              "order": {
                "external_id": 77,
                "number": "ORD-77",
                "url": "https://example.test/orders/77",
                "cost": {
                  "value": 21.8,
                  "currency": "USD"
                },
                "items": [
                  {
                    "external_id": 1,
                    "name": "Chocolate candies",
                    "url": "https://example.test/products/1",
                    "img": "https://example.test/products/1.png",
                    "quantity": {
                      "value": 2,
                      "unit": "pcs"
                    },
                    "price": {
                      "value": 12.9,
                      "currency": "USD"
                    }
                  }
                ],
                "delivery": {
                  "name": "Courier",
                  "address": "221B Baker Street, London",
                  "comment": "Call before delivery",
                  "price": {
                    "value": 5,
                    "currency": "USD"
                  }
                },
                "discount": {
                  "value": 4,
                  "currency": "USD"
                },
                "payments": [
                  {
                    "name": "Card",
                    "amount": {
                      "value": 21.8,
                      "currency": "USD"
                    },
                    "status": {
                      "name": "Paid",
                      "payed": true
                    }
                  }
                ],
                "status": {
                  "code": "complete",
                  "name": "Complete"
                },
                "date": "2026-04-22T12:30:00Z"
              }
            }
            """.trimIndent(),
        )

        val order = message.order
        val item = order?.items?.first()

        assertEquals("104", message.idValue)
        assertEquals("order", message.type)
        assertEquals("77", order?.idValue)
        assertEquals("ORD-77", order?.numberValue)
        assertEquals("https://example.test/orders/77", order?.url)
        assertEquals(21.8, order?.cost?.value ?: 0.0, 0.0)
        assertEquals("Chocolate candies", item?.name)
        assertEquals("1", item?.idValue)
        assertEquals("https://example.test/products/1", item?.url)
        assertEquals(2.0, item?.quantity?.value ?: 0.0, 0.0)
        assertEquals("pcs", item?.quantity?.unit)
        assertEquals(12.9, item?.price?.value ?: 0.0, 0.0)
        assertEquals("USD", item?.price?.currency)
        assertEquals("Courier", order?.delivery?.name)
        assertEquals("221B Baker Street, London", order?.delivery?.address)
        assertEquals(5.0, order?.delivery?.price?.value ?: 0.0, 0.0)
        assertEquals(4.0, order?.discount?.value ?: 0.0, 0.0)
        assertEquals("Card", order?.payments?.first()?.name)
        assertEquals("Paid", order?.payments?.first()?.status?.name)
        assertEquals(true, order?.payments?.first()?.status?.payed)
        assertEquals("complete", order?.status?.code)
        assertEquals("Complete", order?.status?.name)
        assertEquals("2026-04-22T12:30:00Z", order?.date)
    }
}
