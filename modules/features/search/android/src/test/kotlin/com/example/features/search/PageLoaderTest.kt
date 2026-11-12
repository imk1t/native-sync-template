package com.example.features.search

import com.example.shared.github.Page
import com.example.shared.github.RepositorySummary
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PageLoaderTest {
    // @scenario github-search/empty
    // @scenario github-search/pagination
    @Test fun emptyAndPagingRetainContentAfterFailure() = runTest {
        val loader = PageLoader<RepositorySummary> { it.id }
        loader.load(1) { Page(emptyList()) }
        assertTrue(loader.state.value.hasLoaded && loader.state.value.items.isEmpty())
        loader.load(1) { Page(listOf(sample(1)), 2, incomplete = true, limitReached = true) }
        assertTrue(loader.state.value.incomplete && loader.state.value.limitReached)
        loader.load(2) { throw IOException("failed") }
        assertEquals(listOf(1L), loader.state.value.items.map { it.id })
        assertEquals(2, loader.state.value.nextPage)
        assertNotNull(loader.state.value.error)
        loader.load(2) { Page(listOf(sample(1), sample(2))) }
        assertEquals(listOf(1L, 2L), loader.state.value.items.map { it.id })
        assertNull(loader.state.value.nextPage)
        assertNull(loader.state.value.error)
    }

    // @scenario github-search/stale-response
    @Test fun resetRejectsLateResponse() = runTest {
        val gate = CompletableDeferred<Page<RepositorySummary>>()
        val loader = PageLoader<RepositorySummary> { it.id }
        val old = launch { loader.load(1) { gate.await() } }
        runCurrent()
        loader.reset()
        loader.load(1) { Page(listOf(sample(2))) }
        gate.complete(Page(listOf(sample(1))))
        old.join()
        assertEquals(listOf(2L), loader.state.value.items.map { it.id })
        assertNull(loader.state.value.error)
    }

    // @scenario github-search/cancellation
    @Test fun cancellationIsNotAnErrorAndCanReload() = runTest {
        val loader = PageLoader<RepositorySummary> { it.id }
        val pending = launch {
            loader.load(1) {
                delay(60_000)
                Page(listOf(sample(1)))
            }
        }
        runCurrent()
        pending.cancelAndJoin()
        assertFalse(loader.state.value.isLoading)
        assertNull(loader.state.value.error)
        loader.load(1) { Page(listOf(sample(2))) }
        assertEquals(listOf(2L), loader.state.value.items.map { it.id })
    }
}
