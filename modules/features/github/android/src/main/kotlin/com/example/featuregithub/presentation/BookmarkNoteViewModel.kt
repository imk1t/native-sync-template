package com.example.featuregithub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.corenavigation.Navigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NoteState(
    val note: String = "",
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val characterCount: Int get() = note.codePointCount(0, note.length)
    val canSave: Boolean get() = isLoaded && !isSaving && characterCount <= 1_000
}

class BookmarkNoteViewModel(
    repositoryId: String,
    private val bookmarks: BookmarkRepository,
    private val navigator: Navigator
) : ViewModel() {
    private val id = repositoryId.toLongOrNull()
    private val entryID = navigator.state.value.modals.lastOrNull()?.stack?.firstOrNull()?.id
    private val mutableState = MutableStateFlow(NoteState())
    val state: StateFlow<NoteState> = mutableState
    fun edit(note: String) {
        mutableState.value = state.value.copy(note = note)
    }
    fun load() {
        viewModelScope.launch { loadNow() }
    }
    internal suspend fun loadNow() {
        if (state.value.isLoaded) return
        try {
            bookmarks.load()
            val bookmark = bookmarks.items.value.firstOrNull { it.id == id } ?: throw BookmarkError.NotFound()
            mutableState.value = NoteState(note = bookmark.note, isLoaded = true)
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (e: Exception) {
            mutableState.value = state.value.copy(error = e.message ?: "読み込めませんでした。")
        }
    }
    fun save() {
        viewModelScope.launch { saveNow() }
    }
    internal suspend fun saveNow() {
        if (!state.value.canSave || id == null) return
        mutableState.value = state.value.copy(isSaving = true)
        try {
            bookmarks.saveNote(id, state.value.note)
            close()
        } catch (
            e: CancellationException
        ) {
            throw e
        } catch (
            e: Exception
        ) {
            mutableState.value = state.value.copy(error = e.message ?: "保存できませんでした。")
        } finally {
            mutableState.value = state.value.copy(isSaving = false)
        }
    }
    fun cancel() {
        if (!state.value.isSaving) close()
    }
    private fun close() {
        if (entryID != null &&
            navigator.state.value.modals.lastOrNull()?.stack?.firstOrNull()?.id == entryID
        ) {
            navigator.dismiss()
        }
    }
}
