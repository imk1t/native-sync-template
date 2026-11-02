package com.example.featureitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.designsystem.Tokens

@Composable
fun ItemsScreen(viewModel: ItemsViewModel) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadIfNeeded() }
    when (val s = state) {
        ItemsState.Loading -> CircularProgressIndicator()
        is ItemsState.Loaded -> LazyColumn {
            items(s.items, key = { it.id }) { item ->
                Text(
                    item.title,
                    Modifier.fillMaxWidth().clickable { viewModel.select(item) }.padding(Tokens.Spacing.medium)
                )
            }
        }
        ItemsState.Error -> Text("読み込みに失敗しました", color = Tokens.Colors.primary)
    }
}
