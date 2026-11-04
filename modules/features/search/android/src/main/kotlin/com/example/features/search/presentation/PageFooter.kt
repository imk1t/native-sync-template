package com.example.features.search

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.shared.designsystem.EmptyState
import com.example.shared.designsystem.ErrorState
import com.example.shared.designsystem.LoadingIndicator

@Composable
internal fun <T> PageFooter(state: PagedState<T>, retry: () -> Unit, next: () -> Unit) {
    if (state.isLoading) LoadingIndicator()
    state.error?.let { ErrorState(it, retry) }
    if (state.hasLoaded && state.items.isEmpty() && !state.isLoading) {
        EmptyState("結果がありません", "条件を変えて探すか、続きがある場合は次のページを確認してください。")
    }
    if (state.incomplete) Text("検索結果の一部を取得できませんでした。条件を絞って再検索できます。")
    if (state.limitReached) Text("検索結果は最大1,000件まで表示します。")
    if (state.nextPage != null) TextButton(onClick = next, enabled = !state.isLoading) { Text("さらに読み込む") }
}
