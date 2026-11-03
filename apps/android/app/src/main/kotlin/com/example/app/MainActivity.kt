package com.example.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.corenavigation.NavigationState
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import com.example.corenavigation.fromUrl
import com.example.corenetwork.ApiException
import com.example.corenetwork.Item
import com.example.corenetwork.ItemsApi
import com.example.featureitems.ItemDetailScreen
import com.example.featureitems.ItemDetailViewModel
import com.example.featureitems.ItemsScreen
import com.example.featureitems.ItemsViewModel

// 組み立てだけを行う。API サーバーができたら DefaultItemsApi(UrlConnectionTransport("https://...")) に置き換える。
private object FakeItemsApi : ItemsApi {
    private val items = listOf(Item("1", "First"), Item("2", "Second"))

    override suspend fun listItems() = items

    override suspend fun getItem(id: String) = items.firstOrNull { it.id == id } ?: throw ApiException(404)
}

/** Navigator を Activity の ViewModelStore に置き、構成変更でも遷移の状態を保つ。 */
class AppViewModel : ViewModel() {
    val navigator = Navigator()
}

class MainActivity : ComponentActivity() {
    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // システムバーのアイコン色を背景の明暗に合わせる(Android 15 以降の強制 edge-to-edge でも必要)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) open(intent)
        setContent { AppRoot(app.navigator) { route -> Screen(route, app.navigator) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        open(intent)
    }

    // ディープリンク。intent-filter（URL スキームや App Links）の登録はアプリごとに行う。
    private fun open(intent: Intent?) {
        val route = intent?.data?.toString()?.let(Route::fromUrl) ?: return
        app.navigator.reset(NavigationState.opening(route, app.navigator.state.value.selectedTab))
    }
}

/**
 * Route → 画面の対応。全機能の画面を知っているのはここだけ（docs/adr/0002）。
 * ViewModel は Navigation 3 のエントリごとの ViewModelStore に置かれ、スタックから外れると破棄される。
 */
@Composable
private fun Screen(route: Route, navigator: Navigator) {
    when (route) {
        Route.Items -> ItemsScreen(viewModel(factory = ItemsViewModel.factory(FakeItemsApi, navigator)))
        is Route.ItemDetail ->
            ItemDetailScreen(viewModel(factory = ItemDetailViewModel.factory(route.id, FakeItemsApi)))
    }
}
