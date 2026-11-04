package com.example.features.settings

import com.example.core.storage.DisplaySettings
import com.example.core.storage.InMemoryKeyValueStore
import com.example.core.storage.KeyValueStore
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SettingsViewModelTest {
    // @scenario settings/save-restore
    @Test fun persistsSelection() = runTest {
        val store = InMemoryKeyValueStore()
        val model = SettingsViewModel(DisplaySettings(store))
        model.loadNow()
        model.saveNow(false)
        val restored = DisplaySettings(store)
        restored.load()
        assertFalse(restored.showDescriptions.value)
    }

    // @scenario settings/write-fails
    @Test fun failedWriteKeepsSharedSetting() = runTest {
        val settings = DisplaySettings(object : KeyValueStore {
            override suspend fun get(key: String): String? = null
            override suspend fun set(key: String, value: String?): Unit = throw IOException("write failed")
        })
        val model = SettingsViewModel(settings)
        model.loadNow()
        model.saveNow(false)
        assertTrue(settings.showDescriptions.value)
        assertNotNull(model.state.value.error)
    }
}
