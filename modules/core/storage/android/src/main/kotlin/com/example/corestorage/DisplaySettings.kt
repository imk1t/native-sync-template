package com.example.corestorage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DisplaySettings(private val store: KeyValueStore) {
    private val mutex = Mutex()
    private val mutableShowDescriptions = MutableStateFlow(true)
    val showDescriptions: StateFlow<Boolean> = mutableShowDescriptions
    private var isLoaded = false

    suspend fun load() = mutex.withLock { loadLocked() }

    private suspend fun loadLocked() {
        if (isLoaded) return
        mutableShowDescriptions.value = store.get(KEY) != "false"
        isLoaded = true
    }

    suspend fun setShowDescriptions(value: Boolean) = mutex.withLock {
        loadLocked()
        store.set(KEY, value.toString())
        mutableShowDescriptions.value = value
    }

    companion object {
        const val KEY = "display.showDescriptions"
    }
}
