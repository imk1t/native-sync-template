package com.example.corenavigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

// shared/scenarios/navigation.yaml の各 step を実装する
class NavigatorTest {
    private val searchSheet = Modal(ModalStyle.Sheet, listOf(NavigationEntry(Route.RepositorySearch)))

    // @scenario navigation/push-onto-selected-tab
    @Test
    fun pushOntoSelectedTab() {
        val navigator = Navigator()
        val roots = navigator.state.value.roots
        navigator.push(Route.RepositoryDetail("owner", "1"))
        assertEquals(
            listOf(Route.RepositoryDetail("owner", "1")),
            navigator.state.value.stack(AppTab.Home).map {
                it.route
            }
        )
        assertEquals(roots, navigator.state.value.roots)
        assertEquals(emptyList(), navigator.state.value.modals)
    }

    // @scenario navigation/push-inside-modal
    @Test
    fun pushInsideModal() {
        val navigator = Navigator(NavigationState(modals = listOf(searchSheet)))
        navigator.push(Route.RepositoryDetail("owner", "1"))
        assertEquals(emptyList(), navigator.state.value.stack(AppTab.Home))
        assertEquals(
            listOf(Route.RepositorySearch, Route.RepositoryDetail("owner", "1")),
            navigator.state.value.modals[0].stack.map { it.route }
        )
        assertEquals(searchSheet.stack[0], navigator.state.value.modals[0].stack[0])
    }

    // @scenario navigation/pop-modal-root-dismisses
    @Test
    fun popModalRootDismisses() {
        val navigator = Navigator(NavigationState(modals = listOf(searchSheet)))
        navigator.pop()
        assertEquals(emptyList(), navigator.state.value.modals)
    }

    // @scenario navigation/pop-on-empty-stack
    @Test
    fun popOnEmptyStack() {
        val navigator = Navigator()
        val initial = navigator.state.value
        navigator.pop()
        assertEquals(initial, navigator.state.value)
    }

    // @scenario navigation/pop-to-root
    @Test
    fun popToRoot() {
        val navigator = Navigator()
        val root = navigator.state.value.root(AppTab.Home)
        navigator.push(Route.RepositoryDetail("owner", "1"))
        navigator.push(Route.RepositoryDetail("owner", "2"))
        navigator.popToRoot()
        assertEquals(emptyList(), navigator.state.value.stack(AppTab.Home))
        assertEquals(root, navigator.state.value.root(AppTab.Home))
    }

    // @scenario navigation/present-and-dismiss
    @Test
    fun presentAndDismiss() {
        val navigator = Navigator()
        navigator.present(Route.RepositoryDetail("owner", "1"), ModalStyle.FullScreen)
        val first = navigator.state.value.modals[0]
        navigator.present(Route.RepositorySearch)
        navigator.dismiss()
        assertEquals(listOf(first), navigator.state.value.modals)
        assertEquals(ModalStyle.FullScreen, first.style)
        assertEquals(listOf(Route.RepositoryDetail("owner", "1")), first.stack.map { it.route })
    }

    // @scenario navigation/select-tab-dismisses-modals
    @Test
    fun selectTabDismissesModals() {
        val state = NavigationState(
            stacks =
            NavigationState().stacks +
                (AppTab.Home to listOf(NavigationEntry(Route.RepositoryDetail("owner", "1")))),
            modals = listOf(searchSheet)
        )
        val navigator = Navigator(state)
        navigator.selectTab(AppTab.Home)
        assertEquals(AppTab.Home, navigator.state.value.selectedTab)
        assertEquals(state.stacks, navigator.state.value.stacks)
        assertEquals(state.roots, navigator.state.value.roots)
        assertEquals(emptyList(), navigator.state.value.modals)
    }

    // @scenario navigation/open-deep-link
    @Test
    fun openDeepLink() {
        val detail = Route.fromPath("/repositories/owner/a%2F1")!!
        val state = NavigationState.opening(detail, AppTab.Home)
        assertEquals(listOf(Route.RepositoryDetail("owner", "a/1")), state.stack(AppTab.Home).map { it.route })
        val list = Route.fromPath("/search")!!
        assertEquals(emptyList(), NavigationState.opening(list, AppTab.Home).stack(AppTab.Home))
        assertNull(Route.fromPath("/unknown"))
    }

    // @scenario navigation/open-url-with-encoded-id
    @Test
    fun openUrlDecodesEachSegmentOnlyOnce() {
        for ((encoded, id) in listOf("a%2F1" to "a/1", "a%252F1" to "a%2F1", "a%2B1" to "a+1")) {
            val route = Route.fromUrl("https://example.com/repositories/owner/$encoded?source=test#detail")!!
            assertEquals(Route.RepositoryDetail("owner", id), route)
            assertEquals(listOf(route), NavigationState.opening(route, AppTab.Home).stack(AppTab.Home).map { it.route })
        }
        assertNull(Route.fromUrl("https://example.com/unknown"))
        assertNull(Route.fromUrl("https://example.com/repositories/owner/%invalid"))
    }

    // @scenario navigation/duplicate-route-entries
    @Test
    fun duplicateRoutesHaveIndependentStableIdentities() {
        val navigator = Navigator()
        val route = Route.RepositoryDetail("owner", "1")
        navigator.push(route)
        val first = navigator.state.value.stack(AppTab.Home).last()
        navigator.push(route)
        val second = navigator.state.value.stack(AppTab.Home).last()
        assertEquals(first.route, second.route)
        assertNotEquals(first.id, second.id)
        navigator.pop()
        assertEquals(listOf(first), navigator.state.value.stack(AppTab.Home))
        navigator.push(route)
        assertNotEquals(second.id, navigator.state.value.stack(AppTab.Home).last().id)

        navigator.present(route)
        val modalRoot = navigator.state.value.modals[0].stack[0]
        navigator.push(route)
        assertNotEquals(modalRoot.id, navigator.state.value.modals[0].stack[1].id)
        navigator.popToRoot()
        assertEquals(listOf(modalRoot), navigator.state.value.modals[0].stack)
    }

    @Test
    fun stacksMustContainEveryTab() {
        assertFailsWith<IllegalArgumentException> { NavigationState(stacks = emptyMap()) }
    }
}
