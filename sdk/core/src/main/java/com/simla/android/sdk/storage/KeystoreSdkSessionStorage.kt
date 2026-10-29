package com.simla.android.sdk.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.annotation.RestrictTo
import com.simla.android.sdk.CrmSdkInternalApi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Persists session tokens encrypted with an application-private Android Keystore key. */
@CrmSdkInternalApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class KeystoreSdkSessionStorage(
    context: Context,
    private val namespace: String? = null,
) : SdkSessionStorage {
    private val storageDirectory = File(context.noBackupFilesDir, STORAGE_DIRECTORY)
    private val keyProvider = AndroidKeystoreKeyProvider()
    private var activeScopeHash = scopeHash(namespace, ANONYMOUS_SCOPE)

    @Synchronized
    override fun activateScope(scope: String?) {
        val normalizedScope = scope?.trim()?.takeIf(String::isNotEmpty) ?: ANONYMOUS_SCOPE
        activeScopeHash = scopeHash(namespace, normalizedScope)
    }

    @Synchronized
    override fun getUserToken(): String? {
        val tokenFile = tokenFile()
        if (!tokenFile.exists()) return null

        val encrypted = try {
            AtomicFile(tokenFile).openRead().use { it.readBytes() }
        } catch (_: IOException) {
            tokenFile.delete()
            return null
        }
        val token = try {
            AesGcmSessionTokenCodec.decrypt(
                key = keyProvider.getOrCreate(),
                aad = aad(activeScopeHash),
                encrypted = encrypted,
            ).toString(Charsets.UTF_8)
        } catch (_: AEADBadTagException) {
            tokenFile.delete()
            return null
        } catch (_: GeneralSecurityException) {
            tokenFile.delete()
            return null
        } catch (_: IOException) {
            tokenFile.delete()
            return null
        } catch (_: IllegalArgumentException) {
            tokenFile.delete()
            return null
        }
        return token.takeIf(String::isNotBlank) ?: run {
            tokenFile.delete()
            null
        }
    }

    @Synchronized
    override fun setUserToken(value: String?) {
        val normalizedToken = value?.takeIf(String::isNotBlank)
        if (normalizedToken == null) {
            clear()
            return
        }

        val encrypted = encrypt(normalizedToken.toByteArray(Charsets.UTF_8))
        storageDirectory.mkdirs()
        check(storageDirectory.isDirectory) { "Unable to create SDK session storage directory" }
        writeAtomically(AtomicFile(tokenFile()), encrypted)
    }

    @Synchronized
    override fun clear() {
        AtomicFile(tokenFile()).delete()
    }

    private fun encrypt(token: ByteArray): ByteArray = try {
        AesGcmSessionTokenCodec.encrypt(
            key = keyProvider.getOrCreate(),
            aad = aad(activeScopeHash),
            plaintext = token,
        )
    } catch (firstFailure: GeneralSecurityException) {
        keyProvider.delete()
        try {
            AesGcmSessionTokenCodec.encrypt(
                key = keyProvider.getOrCreate(),
                aad = aad(activeScopeHash),
                plaintext = token,
            )
        } catch (retryFailure: GeneralSecurityException) {
            retryFailure.addSuppressed(firstFailure)
            throw retryFailure
        }
    }

    private fun tokenFile(): File = File(storageDirectory, "session_$activeScopeHash.token")

    private fun writeAtomically(file: AtomicFile, value: ByteArray) {
        var output: FileOutputStream? = null
        try {
            output = file.startWrite()
            output.write(value)
            file.finishWrite(output)
        } catch (throwable: Throwable) {
            file.failWrite(output)
            throw throwable
        }
    }

    private class AndroidKeystoreKeyProvider {
        private val keyStore: KeyStore
            get() = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        fun getOrCreate(): SecretKey {
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(KEY_SIZE_BITS)
                        .build(),
                )
                generateKey()
            }
        }

        fun delete() {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    private companion object {
        private const val STORAGE_DIRECTORY = "crm_consultant_sdk"
        private const val ANONYMOUS_SCOPE = "anonymous"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "com.simla.consultant.session.v1"
        private const val KEY_SIZE_BITS = 256

        private fun scopeHash(namespace: String?, scope: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest("${namespace.orEmpty()}\u0000$scope".toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { byte -> "%02x".format(byte) }

        private fun aad(scopeHash: String): ByteArray =
            "crm-session-token:v1:$scopeHash".toByteArray(Charsets.UTF_8)
    }
}

internal object AesGcmSessionTokenCodec {
    private const val MAGIC = 0x43534B31
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val MAX_CIPHERTEXT_BYTES = 64 * 1024

    fun encrypt(key: SecretKey, aad: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintext)
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(cipher.iv.size)
                output.writeInt(ciphertext.size)
                output.write(cipher.iv)
                output.write(ciphertext)
            }
            bytes.toByteArray()
        }
    }

    fun decrypt(key: SecretKey, aad: ByteArray, encrypted: ByteArray): ByteArray {
        val envelope = DataInputStream(ByteArrayInputStream(encrypted))
        require(envelope.readInt() == MAGIC) { "Unsupported session token format" }
        val ivLength = envelope.readInt()
        val ciphertextLength = envelope.readInt()
        require(ivLength == IV_LENGTH_BYTES) { "Invalid session token IV" }
        require(ciphertextLength in 1..MAX_CIPHERTEXT_BYTES) {
            "Invalid session token ciphertext length"
        }
        require(encrypted.size == Int.SIZE_BYTES * 3 + ivLength + ciphertextLength) {
            "Invalid session token envelope length"
        }
        val iv = ByteArray(ivLength).also(envelope::readFully)
        val ciphertext = ByteArray(ciphertextLength).also(envelope::readFully)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }
}
