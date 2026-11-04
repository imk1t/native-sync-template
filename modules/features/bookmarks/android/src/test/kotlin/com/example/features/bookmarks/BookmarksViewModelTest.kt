package com.example.features.bookmarks

import com.example.core.navigation.AppTab
import com.example.core.navigation.Navigator
import com.example.core.navigation.Route
import com.example.core.storage.DisplaySettings
import com.example.core.storage.InMemoryKeyValueStore
import com.example.shared.bookmarks.BookmarkRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class BookmarksViewModelTest {
    @Test fun sharesChangesAndOpensDetailWithoutDependingOnSearch() = runTest {
        val store = InMemoryKeyValueStore()
        val repository = BookmarkRepository(store)
        val navigator = Navigator()
        navigator.selectTab(AppTab.Saved)
        val model = BookmarksViewModel(BookmarksDependencies(repository, DisplaySettings(store)), navigator)
        model.loadNow()
        assertTrue(model.dependencies.bookmarks.items.value.isEmpty())

        repository.toggle(sample())
        repository.saveNote(sample().id, "共有メモ")
        val bookmark = model.dependencies.bookmarks.items.value.single()
        assertEquals("共有メモ", bookmark.note)
        model.select(bookmark)
        assertEquals(Route.RepositoryDetail("owner", "repo"), navigator.state.value.stacks[AppTab.Saved]?.last()?.route)
        model.edit(bookmark)
        assertEquals(
            Route.BookmarkNote(bookmark.id.toString()),
            navigator.state.value.modals.last().stack.first().route
        )

        repository.remove(bookmark.id)
        assertTrue(model.dependencies.bookmarks.items.value.isEmpty())
    }
}
