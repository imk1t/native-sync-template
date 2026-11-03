package com.example.featureitems

import com.example.corenetwork.Item
import com.example.corenetwork.ItemsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext

/** 初回だけキャンセルする。lateResponse はキャンセルを無視して応答する API を再現する。 */
internal class CancelledOnceApi(private val lateResponse: Boolean) : ItemsApi {
    var calls = 0
        private set

    override suspend fun listItems() = listOf(next())

    override suspend fun getItem(id: String) = next()

    private suspend fun next(): Item {
        calls++
        if (calls == 1) {
            if (lateResponse) currentCoroutineContext().cancel() else throw CancellationException()
        }
        return Item("1", "First")
    }
}
