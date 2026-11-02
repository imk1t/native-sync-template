package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.corenetwork.Item
import com.example.corenetwork.ItemsApi
import com.example.featureitems.ItemsScreen
import com.example.featureitems.ItemsViewModel

// 組み立てだけを行う。API の実装は生成クライアントに置き換える（tools/generate.py の拡張）。
private object FakeItemsApi : ItemsApi {
    override suspend fun listItems() = listOf(Item("1", "First"), Item("2", "Second"))
}

class MainActivity : ComponentActivity() {
    private val viewModel: ItemsViewModel by viewModels { ItemsViewModel.factory(FakeItemsApi) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ItemsScreen(viewModel) }
    }
}
