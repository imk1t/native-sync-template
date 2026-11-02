package com.example.corenavigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

// shared/scenarios/navigation.yaml の各 step を実装する
class NavigatorTest {
    private val sheetWithItems = Modal(ModalStyle.Sheet, listOf(Route.Items))

    private fun stacks(vararg home: Route) = mapOf(AppTab.Home to home.toList())

    // @scenario navigation/push-onto-selected-tab
    @Test
    fun pushOntoSelectedTab() {
        val navigator = Navigator()
        navigator.push(Route.ItemDetail("1"))
        assertEquals(NavigationState(stacks = stacks(Route.ItemDetail("1"))), navigator.state.value)
    }

    // @scenario navigation/push-inside-modal
    @Test
    fun pushInsideModal() {
        val navigator = Navigator(NavigationState(modals = listOf(sheetWithItems)))
        navigator.push(Route.ItemDetail("1"))
        assertEquals(emptyList(), navigator.state.value.stack(AppTab.Home))
        assertEquals(
            listOf(Modal(ModalStyle.Sheet, listOf(Route.Items, Route.ItemDetail("1")))),
            navigator.state.value.modals
        )
    }

    // @scenario navigation/pop-modal-root-dismisses
    @Test
    fun popModalRootDismisses() {
        val navigator = Navigator(NavigationState(modals = listOf(sheetWithItems)))
        navigator.pop()
        assertEquals(emptyList(), navigator.state.value.modals)
    }

    // @scenario navigation/pop-on-empty-stack
    @Test
    fun popOnEmptyStack() {
        val navigator = Navigator()
        navigator.pop()
        assertEquals(NavigationState(), navigator.state.value)
    }

    // @scenario navigation/pop-to-root
    @Test
    fun popToRoot() {
        val navigator = Navigator(NavigationState(stacks = stacks(Route.ItemDetail("1"), Route.ItemDetail("2"))))
        navigator.popToRoot()
        assertEquals(emptyList(), navigator.state.value.stack(AppTab.Home))
    }

    // @scenario navigation/present-and-dismiss
    @Test
    fun presentAndDismiss() {
        val navigator = Navigator()
        navigator.present(Route.ItemDetail("1"), ModalStyle.FullScreen)
        navigator.present(Route.Items)
        navigator.dismiss()
        assertEquals(listOf(Modal(ModalStyle.FullScreen, listOf(Route.ItemDetail("1")))), navigator.state.value.modals)
    }

    // @scenario navigation/select-tab-dismisses-modals
    @Test
    fun selectTabDismissesModals() {
        val navigator = Navigator(
            NavigationState(stacks = stacks(Route.ItemDetail("1")), modals = listOf(sheetWithItems))
        )
        navigator.selectTab(AppTab.Home)
        assertEquals(
            NavigationState(selectedTab = AppTab.Home, stacks = stacks(Route.ItemDetail("1"))),
            navigator.state.value
        )
    }

    // @scenario navigation/open-deep-link
    @Test
    fun openDeepLink() {
        val detail = Route.fromPath("/items/a%2F1")!!
        assertEquals(
            NavigationState(stacks = stacks(Route.ItemDetail("a/1"))),
            NavigationState.opening(detail, AppTab.Home)
        )
        val list = Route.fromPath("/items")!!
        assertEquals(NavigationState(selectedTab = AppTab.Home), NavigationState.opening(list, AppTab.Home))
        assertNull(Route.fromPath("/unknown"))
    }

    @Test
    fun stacksMustContainEveryTab() {
        assertFailsWith<IllegalArgumentException> { NavigationState(stacks = emptyMap()) }
    }
}
