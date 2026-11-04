package com.example.featuresearch

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.corelocalization.Message
import com.example.corelocalization.localized
import com.example.designsystem.EmptyState
import com.example.designsystem.ErrorState
import com.example.designsystem.LoadingIndicator

@Composable
internal fun <T> PageFooter(state: PagedState<T>, retry: () -> Unit, next: () -> Unit) {
    if (state.isLoading) LoadingIndicator(Message.CommonLoading.localized())
    state.error?.let { ErrorState(it.localized(), Message.CommonRetry.localized(), retry) }
    if (state.hasLoaded && state.items.isEmpty() && !state.isLoading) {
        EmptyState(Message.SearchEmptyTitle.localized(), Message.SearchEmptyMessage.localized())
    }
    if (state.incomplete) Text(Message.SearchIncomplete.localized())
    if (state.limitReached) Text(Message.SearchLimit.localized())
    if (state.nextPage !=
        null
    ) {
        TextButton(onClick = next, enabled = !state.isLoading) { Text(Message.SearchMore.localized()) }
    }
}
