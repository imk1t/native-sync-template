package com.example.corenavigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ModalStyle { Sheet, FullScreen }

/** stack の先頭がモーダルで開いた画面。モーダル表示中の push はここに積む。 */
data class Modal(val style: ModalStyle, val stack: List<Route>)

/** 画面遷移の状態。タブごとのスタック（タブの先頭の画面は含めない）と、下から順に重なったモーダル。 */
data class NavigationState(
    val selectedTab: AppTab = AppTab.entries.first(),
    val stacks: Map<AppTab, List<Route>> = AppTab.entries.associateWith { emptyList() },
    val modals: List<Modal> = emptyList()
) {
    init {
        require(stacks.keys == AppTab.entries.toSet()) { "stacks には全タブを含めてください: ${stacks.keys}" }
    }

    fun stack(tab: AppTab): List<Route> = stacks.getValue(tab)

    companion object {
        /** ディープリンク用。route がタブの先頭ならそのタブを、それ以外は tab の上に route を開く。 */
        fun opening(route: Route, tab: AppTab): NavigationState {
            val root = AppTab.entries.firstOrNull { it.root == route }
            if (root != null) return NavigationState(selectedTab = root)
            return NavigationState(selectedTab = tab, stacks = NavigationState().stacks + (tab to listOf(route)))
        }
    }
}

/** 画面遷移の操作。すべての遷移はここを通す（docs/adr/0002）。 */
class Navigator(initial: NavigationState = NavigationState()) {
    private val mutableState = MutableStateFlow(initial)
    val state: StateFlow<NavigationState> = mutableState.asStateFlow()

    fun push(route: Route) = mutableState.update { s ->
        if (s.modals.isEmpty()) {
            s.withStack(s.stack(s.selectedTab) + route)
        } else {
            s.withTopModal { it.copy(stack = it.stack + route) }
        }
    }

    fun pop() = mutableState.update { s ->
        when {
            s.modals.isNotEmpty() && s.modals.last().stack.size > 1 ->
                s.withTopModal { it.copy(stack = it.stack.dropLast(1)) }
            s.modals.isNotEmpty() -> s.copy(modals = s.modals.dropLast(1))
            else -> s.withStack(s.stack(s.selectedTab).dropLast(1))
        }
    }

    fun popToRoot() = mutableState.update { s ->
        if (s.modals.isEmpty()) {
            s.withStack(emptyList())
        } else {
            s.withTopModal { it.copy(stack = it.stack.take(1)) }
        }
    }

    fun present(route: Route, style: ModalStyle = ModalStyle.Sheet) = mutableState.update { s ->
        s.copy(modals = s.modals + Modal(style, listOf(route)))
    }

    fun dismiss() = mutableState.update { s -> s.copy(modals = s.modals.dropLast(1)) }

    fun selectTab(tab: AppTab) = mutableState.update { s -> s.copy(selectedTab = tab, modals = emptyList()) }

    fun reset(state: NavigationState) {
        mutableState.value = state
    }

    private fun NavigationState.withStack(stack: List<Route>) = copy(stacks = stacks + (selectedTab to stack))

    private fun NavigationState.withTopModal(transform: (Modal) -> Modal) =
        copy(modals = modals.dropLast(1) + transform(modals.last()))
}
