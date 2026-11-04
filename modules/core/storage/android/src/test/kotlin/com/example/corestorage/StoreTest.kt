package com.example.corestorage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/** 反転するだけの暗号。JVM には AndroidKeyStore が無いため、暗号化を経由していることだけを確かめる。 */
private object ReversingCipher : ValueCipher {
    override fun encrypt(plain: ByteArray) = plain.reversedArray()
    override fun decrypt(encrypted: ByteArray) = encrypted.reversedArray()
}

@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest {
    private val dir: File = Files.createTempDirectory("core-storage").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun TestScope.dataStore(name: String) =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(dir, "$name.preferences_pb") }

    @Test
    fun dataStoreKeyValueStoreSetGetAndRemove() = runTest(UnconfinedTestDispatcher()) {
        val store = DataStoreKeyValueStore(dataStore("kv"))
        assertNull(store.get("theme"))
        store.set("theme", "dark")
        assertEquals("dark", store.get("theme"))
        store.set("theme", null)
        assertNull(store.get("theme"))
    }

    @Test
    fun dataSurvivesStoreRecreation() = runTest(UnconfinedTestDispatcher()) {
        val file = File(dir, "restored.preferences_pb")
        val job = SupervisorJob()
        val first = PreferenceDataStoreFactory.create(scope = CoroutineScope(backgroundScope.coroutineContext + job)) {
            file
        }
        DataStoreKeyValueStore(first).set("github.bookmarks.v1", "{\"version\":1,\"items\":[]}")
        job.cancelAndJoin()
        val second = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        assertEquals("{\"version\":1,\"items\":[]}", DataStoreKeyValueStore(second).get("github.bookmarks.v1"))
    }

    @Test
    fun keystoreSecureStoreSavesOnlyEncryptedValue() = runTest(UnconfinedTestDispatcher()) {
        val dataStore = dataStore("secure")
        val store = KeystoreSecureStore(dataStore, ReversingCipher)
        assertNull(store.get("token"))
        store.set("token", "a")
        store.set("token", "secret")
        assertEquals("secret", store.get("token"))
        assertNotEquals("secret", DataStoreKeyValueStore(dataStore).get("token"))
        store.set("token", null)
        assertNull(store.get("token"))
    }

    @Test
    fun failedOverwritePreservesExistingValue() = runTest(UnconfinedTestDispatcher()) {
        var failEncryption = false
        val cipher = object : ValueCipher {
            override fun encrypt(plain: ByteArray): ByteArray {
                check(!failEncryption) { "encryption failed" }
                return ReversingCipher.encrypt(plain)
            }

            override fun decrypt(encrypted: ByteArray) = ReversingCipher.decrypt(encrypted)
        }
        val store = KeystoreSecureStore(dataStore("overwrite"), cipher)
        store.set("token", "old-token")
        failEncryption = true
        assertFailsWith<IllegalStateException> { store.set("token", "new-token") }
        assertEquals("old-token", store.get("token"))
    }

    @Test
    fun inMemoryStoresBehaveTheSame() = runTest {
        val keyValue = InMemoryKeyValueStore(mapOf("a" to "1"))
        val secure = InMemorySecureStore(mapOf("a" to "1"))
        keyValue.set("a", null)
        secure.set("a", null)
        assertNull(keyValue.get("a"))
        assertNull(secure.get("a"))
    }
}
