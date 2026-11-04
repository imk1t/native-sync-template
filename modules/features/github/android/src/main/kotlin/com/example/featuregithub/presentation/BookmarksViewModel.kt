package com.example.featuregithub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.corenavigation.ModalStyle
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class BookmarksViewModel(val dependencies: GitHubDependencies, private val navigator: Navigator) :
    ViewModel() {
    private val mutableError = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = mutableError
    private val mutableLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = mutableLoading
    private val mutableLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = mutableLoaded
    fun load() {
        viewModelScope.launch { loadNow() }
    }
    internal suspend fun loadNow() {
        if (isLoading.value) return
        mutableLoading.value = true
        try {
            dependencies.loadLocalData()
            mutableError.value = null
            mutableLoaded.value = true
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (
            e: Exception
        ) {
            mutableError.value = "保存データの読み込みに失敗しました。再試行してください。"
        } finally {
            mutableLoading.value = false
        }
    }
    fun select(bookmark: Bookmark) =
        navigator.push(Route.RepositoryDetail(bookmark.repository.owner, bookmark.repository.name))
    fun edit(bookmark: Bookmark) = navigator.present(Route.BookmarkNote(bookmark.id.toString()), ModalStyle.Sheet)
    fun remove(id: Long) {
        viewModelScope.launch {
            try {
                dependencies.bookmarks.remove(id)
                mutableError.value = null
            } catch (
                e: CancellationException
            ) {
                throw e
            } catch (e: Exception) {
                mutableError.value = "削除を保存できませんでした。再試行してください。"
            }
        }
    }
}
