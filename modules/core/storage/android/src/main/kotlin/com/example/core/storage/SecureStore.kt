package com.example.core.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** 認証トークンなどの秘密情報を保存する。KeyValueStore と取り違えないよう型を分ける。 */
interface SecureStore {
    suspend fun get(key: String): String?

    /** value が null なら削除する。 */
    suspend fun set(key: String, value: String?)
}

/** 値の暗号化。本番は AndroidKeystoreCipher、JVM のテストでは差し替える。 */
interface ValueCipher {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(encrypted: ByteArray): ByteArray
}

/** Keystore の鍵で暗号化した値を DataStore に保存する SecureStore（iOS の Keychain に当たる）。 */
class KeystoreSecureStore(dataStore: DataStore<Preferences>, private val cipher: ValueCipher) : SecureStore {
    private val store = DataStoreKeyValueStore(dataStore)

    override suspend fun get(key: String): String? =
        store.get(key)?.let { String(cipher.decrypt(Base64.getDecoder().decode(it)), Charsets.UTF_8) }

    override suspend fun set(key: String, value: String?) {
        val encoded = value?.let {
            Base64.getEncoder().encodeToString(cipher.encrypt(it.toByteArray(Charsets.UTF_8)))
        }
        store.set(key, encoded)
    }
}

/** Android Keystore の AES-GCM 鍵を使う。鍵は端末の外に出ない。出力は IV（12 バイト）+ 暗号文。 */
class AndroidKeystoreCipher(private val alias: String) : ValueCipher {
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return cipher.iv + cipher.doFinal(plain)
    }

    override fun decrypt(encrypted: ByteArray): ByteArray {
        val iv = encrypted.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        }
        return cipher.doFinal(encrypted.copyOfRange(IV_SIZE, encrypted.size))
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}

/** テストやプレビュー用。 */
class InMemorySecureStore(values: Map<String, String> = emptyMap()) : SecureStore {
    private val delegate = InMemoryKeyValueStore(values)

    override suspend fun get(key: String): String? = delegate.get(key)

    override suspend fun set(key: String, value: String?) = delegate.set(key, value)
}
