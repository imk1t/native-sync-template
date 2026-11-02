package com.example.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.corenavigation.AppTab
import com.example.corenavigation.Modal
import com.example.corenavigation.ModalStyle
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route

/** Navigator の状態を描くだけ（docs/adr/0002）。タブ・スタック・モーダルは OS の部品に任せる。 */
@Composable
fun AppRoot(navigator: Navigator, screen: @Composable (Route) -> Unit) {
    val state by navigator.state.collectAsState()
    Box(Modifier.fillMaxSize()) {
        // 画面幅に応じてボトムバー・レール・ドロワーを切り替える
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppTab.entries.forEach { tab ->
                    item(
                        selected = tab == state.selectedTab,
                        onClick = { navigator.selectTab(tab) },
                        icon = {},
                        label = { Text(tab.title) }
                    )
                }
            }
        ) {
            Stack(listOf(state.selectedTab.root) + state.stack(state.selectedTab), navigator, screen)
        }
        ModalLayer(state.modals, 0, navigator, screen)
    }
}

/** スタック 1 本。戻る操作は Navigator.pop に渡す。 */
@Composable
private fun Stack(backStack: List<Route>, navigator: Navigator, screen: @Composable (Route) -> Unit) {
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.pop() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = { route -> NavEntry(route) { screen(route) } }
    )
}

/** modals[level] をボトムシートか全画面ダイアログで出し、その中で次の level を重ねる。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalLayer(modals: List<Modal>, level: Int, navigator: Navigator, screen: @Composable (Route) -> Unit) {
    val modal = modals.getOrNull(level) ?: return
    val content: @Composable () -> Unit = {
        Stack(modal.stack, navigator, screen)
        ModalLayer(modals, level + 1, navigator, screen)
    }
    when (modal.style) {
        ModalStyle.Sheet -> ModalBottomSheet(onDismissRequest = { navigator.dismiss() }) { content() }
        ModalStyle.FullScreen -> Dialog(
            onDismissRequest = { navigator.dismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(Modifier.fillMaxSize()) { Box { content() } }
        }
    }
}
