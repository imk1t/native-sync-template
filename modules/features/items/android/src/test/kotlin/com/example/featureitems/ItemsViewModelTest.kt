package com.example.featureitems

import com.example.corenetwork.Item
import com.example.corenetwork.ItemsApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class StubApi(private val result: Result<List<Item>>) : ItemsApi {
    override suspend fun listItems(): List<Item> = result.getOrThrow()
}

// shared/scenarios/items-list.yaml の各 step を実装する
@OptIn(ExperimentalCoroutinesApi::class)
class ItemsViewModelTest {
    // @scenario items-list/load-succeeds
    @Test
    fun loadSucceeds() = runTest {
        val items = listOf(Item("1", "First"), Item("2", "Second"))
        val vm = ItemsViewModel(StubApi(Result.success(items)))
        vm.loadNow()
        assertEquals(ItemsState.Loaded(items), vm.state.value)
    }

    // @scenario items-list/load-fails
    @Test
    fun loadFails() = runTest {
        val vm = ItemsViewModel(StubApi(Result.failure(RuntimeException())))
        vm.loadNow()
        assertEquals(ItemsState.Error, vm.state.value)
    }

    // @scenario items-list/reappear-in-same-scope
    // @scenario items-list/explicit-reload
    @Test
    fun sameStoreKeepsModelAndStateUntilExplicitReload() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            var calls = 0
            val api = object : ItemsApi {
                override suspend fun listItems(): List<Item> {
                    calls++
                    return listOf(Item("1", "First"))
                }
            }
            val factory = ItemsViewModel.factory(api)
            val first = ViewModelProvider.create(store, factory)[ItemsViewModel::class]
            first.loadIfNeeded()
            advanceUntilIdle()
            // Activity の構成変更後も同じ store から取得する。
            val recreated = ViewModelProvider.create(store, factory)[ItemsViewModel::class]
            recreated.loadIfNeeded()
            advanceUntilIdle()
            assertSame(first, recreated)
            assertEquals(1, calls)
            assertEquals(ItemsState.Loaded(listOf(Item("1", "First"))), recreated.state.value)
            recreated.load()
            advanceUntilIdle()
            assertEquals(2, calls)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    // @scenario items-list/cancel-initial-load
    @Test
    fun clearingStoreCancelsPendingLoadWithoutShowingError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            var cancelled = false
            val api = object : ItemsApi {
                override suspend fun listItems(): List<Item> {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled = true
                    }
                }
            }
            val vm = ViewModelProvider.create(store, ItemsViewModel.factory(api))[ItemsViewModel::class]
            vm.loadIfNeeded()
            runCurrent()
            store.clear()
            runCurrent()
            assertTrue(cancelled)
            assertEquals(ItemsState.Loading, vm.state.value)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
