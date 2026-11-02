package com.example.featureitems

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.corenavigation.AppTab
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import com.example.corenetwork.Item
import com.example.corenetwork.ItemsApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private class StubApi(private val result: Result<List<Item>>) : ItemsApi {
    override suspend fun listItems(): List<Item> = result.getOrThrow()

    override suspend fun getItem(id: String): Item = throw UnsupportedOperationException()
}

// shared/scenarios/items-list.yaml の各 step を実装する
@OptIn(ExperimentalCoroutinesApi::class)
class ItemsViewModelTest {
    // @scenario items-list/load-succeeds
    @Test
    fun loadSucceeds() = runTest {
        val items = listOf(Item("1", "First"), Item("2", "Second"))
        val vm = ItemsViewModel(StubApi(Result.success(items)), Navigator())
        vm.loadNow()
        assertEquals(ItemsState.Loaded(items), vm.state.value)
    }

    // @scenario items-list/load-fails
    @Test
    fun loadFails() = runTest {
        val vm = ItemsViewModel(StubApi(Result.failure(RuntimeException())), Navigator())
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

                override suspend fun getItem(id: String): Item = throw UnsupportedOperationException()
            }
            val factory = ItemsViewModel.factory(api, Navigator())
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

    // @scenario items-list/select-opens-detail
    @Test
    fun selectOpensDetail() {
        val navigator = Navigator()
        val viewModel = ItemsViewModel(StubApi(Result.success(emptyList())), navigator)
        viewModel.select(Item("1", "First"))
        assertEquals(listOf(Route.ItemDetail("1")), navigator.state.value.stack(AppTab.Home))
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

                override suspend fun getItem(id: String): Item = throw UnsupportedOperationException()
            }
            val vm = ViewModelProvider.create(store, ItemsViewModel.factory(api, Navigator()))[ItemsViewModel::class]
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
