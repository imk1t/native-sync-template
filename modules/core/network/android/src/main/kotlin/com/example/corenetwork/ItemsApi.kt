package com.example.corenetwork

data class Item(val id: String, val title: String)

interface ItemsApi {
    suspend fun listItems(): List<Item>
}
