package com.example.featureitems

import com.example.corenetwork.Item
import com.example.corenetwork.ItemsApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class StubApi(private val result: Result<List<Item>>) : ItemsApi {
    override suspend fun listItems(): List<Item> = result.getOrThrow()
}

// shared/scenarios/items-list.yaml と同じ内容
class ItemsViewModelTest {
    @Test
    fun loadSucceeds() = runTest {
        val items = listOf(Item("1", "First"), Item("2", "Second"))
        val vm = ItemsViewModel(StubApi(Result.success(items)))
        vm.loadNow()
        assertEquals(ItemsState.Loaded(items), vm.state.value)
    }

    @Test
    fun loadFails() = runTest {
        val vm = ItemsViewModel(StubApi(Result.failure(RuntimeException())))
        vm.loadNow()
        assertEquals(ItemsState.Error, vm.state.value)
    }
}
