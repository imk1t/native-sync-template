package com.example.corestorage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 設定などの普通の値を保存する。秘密情報は SecureStore に保存する。 */
interface KeyValueStore {
    suspend fun get(key: String): String?

    /** value が null なら削除する。 */
    suspend fun set(key: String, value: String?)
}

/** Preferences DataStore を使う KeyValueStore。 */
class DataStoreKeyValueStore(private val dataStore: DataStore<Preferences>) : KeyValueStore {
    override suspend fun get(key: String): String? = dataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun set(key: String, value: String?) {
        dataStore.edit { prefs ->
            val k = stringPreferencesKey(key)
            if (value == null) prefs.remove(k) else prefs[k] = value
        }
    }

    companion object {
        /** DataStore は 1 ファイルにつき 1 インスタンスにする必要がある。アプリで 1 回だけ呼んで使い回す。 */
        fun create(context: Context, name: String): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.applicationContext.preferencesDataStoreFile(name) }
    }
}

/** テストやプレビュー用。 */
class InMemoryKeyValueStore(values: Map<String, String> = emptyMap()) : KeyValueStore {
    private val mutex = Mutex()
    private val values = values.toMutableMap()

    override suspend fun get(key: String): String? = mutex.withLock { values[key] }

    override suspend fun set(key: String, value: String?) {
        mutex.withLock { if (value == null) values.remove(key) else values[key] = value }
    }
}
