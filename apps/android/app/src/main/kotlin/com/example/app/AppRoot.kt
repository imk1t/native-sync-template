package com.example.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.core.localization.Message
import com.example.core.localization.localized
import com.example.core.navigation.AppTab
import com.example.core.navigation.Modal
import com.example.core.navigation.ModalStyle
import com.example.core.navigation.NavigationEntry
import com.example.core.navigation.Navigator
import com.example.core.navigation.Route
import com.example.shared.designsystem.DesignBackButton
import com.example.shared.designsystem.DesignIcons

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
                                AppTab.Home -> DesignIcons.Search
                                AppTab.Saved -> DesignIcons.Bookmark
                                AppTab.Settings -> DesignIcons.Settings
                            }
                            Icon(icon, contentDescription = null)
                        },
                        label = {
                            Text(
                                when (tab) {
                                    AppTab.Home -> Message.SearchTitle
                                    AppTab.Saved -> Message.CommonSaved
                                    AppTab.Settings -> Message.SettingsTitle
                                }.localized()
                            )
                        }
                    )
                }
            }
        ) {
            // edge-to-edge なので、コンテンツがステータスバーやカメラ切り欠きに重ならないよう上端を避ける
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
                NavDisplay(entries = entriesByTab.getValue(state.selectedTab), onBack = navigator::pop)
                // 戻るボタンは、各画面の中央タイトル（DesignNavTitle）と同じ高さに重ねる
                if (state.stack(state.selectedTab).isNotEmpty()) {
                    DesignBackButton(
                        Message.CommonBack.localized(),
                        navigator::pop,
                        Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 20.dp)
                    )
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
