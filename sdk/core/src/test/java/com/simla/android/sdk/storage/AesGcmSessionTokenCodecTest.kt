package com.simla.android.sdk.storage

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator

class AesGcmSessionTokenCodecTest {
    private val key = KeyGenerator.getInstance("AES").run {
        init(256)
        generateKey()
    }

    @Test
    fun encrypted_token_round_trips_with_matching_aad() {
        val aad = "crm-session-token:v1:scope-a".toByteArray()
        val plaintext = "secret-user-token".toByteArray()

        val encrypted = AesGcmSessionTokenCodec.encrypt(key, aad, plaintext)

        assertArrayEquals(plaintext, AesGcmSessionTokenCodec.decrypt(key, aad, encrypted))
    }

    @Test
    fun encrypted_token_cannot_be_opened_in_another_scope() {
        val encrypted = AesGcmSessionTokenCodec.encrypt(
            key,
            "crm-session-token:v1:scope-a".toByteArray(),
            "secret-user-token".toByteArray(),
        )

        assertThrows(AEADBadTagException::class.java) {
            AesGcmSessionTokenCodec.decrypt(
                key,
                "crm-session-token:v1:scope-b".toByteArray(),
                encrypted,
            )
        }
    }

    @Test
    fun modified_ciphertext_is_rejected() {
        val aad = "crm-session-token:v1:scope-a".toByteArray()
        val encrypted = AesGcmSessionTokenCodec.encrypt(
            key,
            aad,
            "secret-user-token".toByteArray(),
        )
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()

        assertThrows(AEADBadTagException::class.java) {
            AesGcmSessionTokenCodec.decrypt(key, aad, encrypted)
        }
    }

    @Test
    fun malformed_envelope_is_rejected_before_decryption() {
        assertThrows(IllegalArgumentException::class.java) {
            AesGcmSessionTokenCodec.decrypt(
                key,
                "crm-session-token:v1:scope-a".toByteArray(),
                byteArrayOf(1, 2, 3, 4),
            )
        }
    }
}
