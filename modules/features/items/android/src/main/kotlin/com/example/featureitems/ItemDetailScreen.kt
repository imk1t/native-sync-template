package com.example.featureitems

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.designsystem.Tokens

@Composable
fun ItemDetailScreen(viewModel: ItemDetailViewModel) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadIfNeeded() }
    when (val s = state) {
        ItemDetailState.Loading -> CircularProgressIndicator()
        is ItemDetailState.Loaded -> Column(Modifier.padding(Tokens.Spacing.medium)) {
            Text(s.item.title, style = MaterialTheme.typography.titleLarge)
            Text(s.item.id)
        }
        ItemDetailState.Error -> Text("読み込みに失敗しました", color = Tokens.Colors.primary)
    }
}
