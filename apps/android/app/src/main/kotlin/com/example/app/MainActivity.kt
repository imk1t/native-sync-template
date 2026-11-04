package com.example.app

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.corenavigation.NavigationState
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import com.example.corenavigation.fromUrl
import com.example.corenetwork.DefaultGitHubApi
import com.example.corenetwork.GitHubTransport
import com.example.corenetwork.UrlConnectionTransport
import com.example.corestorage.DataStoreKeyValueStore
import com.example.corestorage.DisplaySettings
import com.example.featuregithub.BookmarkNoteScreen
import com.example.featuregithub.BookmarkNoteViewModel
import com.example.featuregithub.BookmarkRepository
import com.example.featuregithub.BookmarksScreen
import com.example.featuregithub.BookmarksViewModel
import com.example.featuregithub.GitHubDependencies
import com.example.featuregithub.GitHubRepositoryImpl
import com.example.featuregithub.IssueDetailScreen
import com.example.featuregithub.IssueDetailViewModel
import com.example.featuregithub.RepositoryDetailScreen
import com.example.featuregithub.RepositoryDetailViewModel
import com.example.featuregithub.RepositoryIssuesScreen
import com.example.featuregithub.RepositoryIssuesViewModel
import com.example.featuregithub.RepositorySearchScreen
import com.example.featuregithub.RepositorySearchViewModel
import com.example.featuresettings.SettingsScreen
import com.example.featuresettings.SettingsViewModel

/** アプリの寿命で共有する依存を組み立てる。複数の Activity からも同じ Store を使う。 */
class SampleApplication : Application() {
    val github by lazy {
        val store = DataStoreKeyValueStore.persistent(this, "github-explorer")
        val transport = UrlConnectionTransport("https://api.github.com")
        GitHubDependencies(
            GitHubRepositoryImpl(DefaultGitHubApi(GitHubTransport(transport))),
            BookmarkRepository(store),
            DisplaySettings(store)
        )
    }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val navigator = Navigator()
    val dependencies = getApplication<SampleApplication>().github
}

class MainActivity : ComponentActivity() {
    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) open(intent)
        setContent {
            // システムバーの外観と同じ OS 設定で、コンテンツの配色も切り替える。
            val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colorScheme) {
                AppRoot(app.navigator) { route -> Screen(route, app.dependencies, app.navigator) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        open(intent)
    }

    private fun open(intent: Intent?) {
        val route = intent?.data?.toString()?.let(Route::fromUrl) ?: return
        app.navigator.reset(NavigationState.opening(route, app.navigator.state.value.selectedTab))
    }
}

@Composable
private fun Screen(route: Route, dependencies: GitHubDependencies, navigator: Navigator) {
    when (route) {
        Route.RepositorySearch -> RepositorySearchScreen(
            viewModel(
                factory = viewModelFactory {
                    initializer { RepositorySearchViewModel(dependencies, navigator) }
                }
            )
        )
        is Route.RepositoryDetail -> RepositoryDetailScreen(
            viewModel(
                factory = viewModelFactory {
                    initializer { RepositoryDetailViewModel(route.owner, route.repo, dependencies, navigator) }
                }
            )
        )
        is Route.RepositoryIssues -> RepositoryIssuesScreen(
            viewModel(
                factory = viewModelFactory {
                    initializer {
                        RepositoryIssuesViewModel(route.owner, route.repo, dependencies.repository, navigator)
                    }
                }
            )
        )
        is Route.IssueDetail -> IssueDetailScreen(
            viewModel(
                factory = viewModelFactory {
                    initializer { IssueDetailViewModel(route.owner, route.repo, route.number, dependencies.repository) }
                }
            )
        )
        Route.Bookmarks -> BookmarksScreen(
            viewModel(
                factory = viewModelFactory {
                    initializer { BookmarksViewModel(dependencies, navigator) }
                }
            )
        )
        is Route.BookmarkNote -> BookmarkNoteScreen(
            viewModel(
                factory = viewModelFactory {
                    initializer { BookmarkNoteViewModel(route.repositoryId, dependencies.bookmarks, navigator) }
                }
            )
        )
        Route.Settings -> SettingsScreen(
            viewModel(
                factory = viewModelFactory {
                    initializer { SettingsViewModel(dependencies.settings) }
                }
            )
        )
    }
}
