package com.example.corenetwork

data class Item(val id: String, val title: String)

interface ItemsApi {
    suspend fun listItems(): List<Item>
}

/** 生成された ApiClient を包み、DTO をドメインモデルに変換する。 */
class DefaultItemsApi(transport: HttpTransport) : ItemsApi {
    private val client = ApiClient(transport)

    override suspend fun listItems(): List<Item> = client.listItems().map { Item(it.id, it.title) }
}
