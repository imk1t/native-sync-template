package com.example.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.corenavigation.AppTab
import com.example.corenavigation.Modal
import com.example.corenavigation.ModalStyle
import com.example.corenavigation.NavigationEntry
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route

/** Navigator の状態を描くだけ（docs/adr/0002）。タブ・スタック・モーダルは OS の部品に任せる。 */
@Composable
fun AppRoot(navigator: Navigator, screen: @Composable (Route) -> Unit) {
    val state by navigator.state.collectAsState()
    // 非表示のタブも decorator を保持し、切り替え時に ViewModelStore を破棄しない。
    val entriesByTab = AppTab.entries.associateWith { tab ->
        key(tab) {
            rememberDecoratedNavEntries(
                backStack = listOf(state.root(tab)) + state.stack(tab),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = { entry -> NavEntry(entry, contentKey = entry.id) { screen(it.route) } }
            )
        }
    }
    Box(Modifier.fillMaxSize()) {
        // 画面幅に応じてボトムバー・レール・ドロワーを切り替える
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppTab.entries.forEach { tab ->
                    item(
                        selected = tab == state.selectedTab,
                        onClick = { navigator.selectTab(tab) },
                        icon = {
                            val icon = when (tab) {
                                AppTab.Home -> android.R.drawable.ic_menu_search
                                AppTab.Saved -> android.R.drawable.ic_menu_save
                                AppTab.Settings -> android.R.drawable.ic_menu_preferences
                            }
                            Icon(painterResource(icon), contentDescription = null)
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        ) {
            // edge-to-edge なので、コンテンツがステータスバーやカメラ切り欠きに重ならないよう上端を避ける
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
                Column {
                    if (state.stack(state.selectedTab).isNotEmpty()) {
                        TextButton(onClick = navigator::pop) { Text("戻る") }
                    }
                    NavDisplay(entries = entriesByTab.getValue(state.selectedTab), onBack = navigator::pop)
                }
            }
        }
        ModalLayer(state.modals, 0, navigator, screen)
    }
}

/** スタック 1 本。戻る操作は Navigator.pop に渡す。 */
@Composable
private fun Stack(backStack: List<NavigationEntry>, navigator: Navigator, screen: @Composable (Route) -> Unit) {
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.pop() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = { entry -> NavEntry(entry, contentKey = entry.id) { screen(it.route) } }
    )
}

/** modals[level] をボトムシートか全画面ダイアログで出し、その中で次の level を重ねる。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalLayer(modals: List<Modal>, level: Int, navigator: Navigator, screen: @Composable (Route) -> Unit) {
    val modal = modals.getOrNull(level) ?: return
    val content: @Composable () -> Unit = {
        key(modal.stack.first().id) {
            Stack(modal.stack, navigator, screen)
            ModalLayer(modals, level + 1, navigator, screen)
        }
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
