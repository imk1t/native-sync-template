package com.example.features.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.core.localization.Message
import com.example.core.localization.localized
import com.example.shared.designsystem.EmptyState
import com.example.shared.designsystem.ErrorState
import com.example.shared.designsystem.LoadingIndicator

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
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(
                onClick = next,
                enabled = !state.isLoading,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) { Text(Message.SearchMore.localized()) }
        }
    }
}
