package com.example.featureitems

import com.example.corenetwork.Item
import com.example.corenetwork.ItemsApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

private class DetailStubApi(private val result: Result<Item>) : ItemsApi {
    override suspend fun listItems(): List<Item> = emptyList()

    override suspend fun getItem(id: String): Item = result.getOrThrow()
}

// shared/scenarios/item-detail.yaml の各 step を実装する
class ItemDetailViewModelTest {
    // @scenario item-detail/load-succeeds
    @Test
    fun loadSucceeds() = runTest {
        val viewModel = ItemDetailViewModel("1", DetailStubApi(Result.success(Item("1", "First"))))
        viewModel.loadNow()
        assertEquals(ItemDetailState.Loaded(Item("1", "First")), viewModel.state.value)
    }

    // @scenario item-detail/load-fails
    @Test
    fun loadFails() = runTest {
        val viewModel = ItemDetailViewModel("1", DetailStubApi(Result.failure(RuntimeException())))
        viewModel.loadNow()
        assertEquals(ItemDetailState.Error, viewModel.state.value)
    }
}
