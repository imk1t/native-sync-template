package com.example.app

import androidx.compose.runtime.Composable
import com.example.core.navigation.Navigator
import com.example.core.navigation.Route
import com.example.features.bookmarks.BookmarkNoteScreen
import com.example.features.bookmarks.BookmarksScreen
import com.example.features.search.IssueDetailScreen
import com.example.features.search.RepositoryDetailScreen
import com.example.features.search.RepositoryIssuesScreen
import com.example.features.search.RepositorySearchScreen
import com.example.features.settings.SettingsScreen

@Composable
internal fun AppRouter(route: Route, dependencies: AppDependencies, navigator: Navigator) {
    when (route) {
        Route.RepositorySearch -> RepositorySearchScreen(dependencies.search, navigator)
        is Route.RepositoryDetail -> RepositoryDetailScreen(route.owner, route.repo, dependencies.search, navigator)
        is Route.RepositoryIssues -> RepositoryIssuesScreen(route.owner, route.repo, dependencies.search, navigator)
        is Route.IssueDetail -> IssueDetailScreen(route.owner, route.repo, route.number, dependencies.search)
        Route.Bookmarks -> BookmarksScreen(dependencies.bookmarks, navigator)
        is Route.BookmarkNote -> BookmarkNoteScreen(route.repositoryId, dependencies.bookmarks, navigator)
        Route.Settings -> SettingsScreen(dependencies.settings)
    }
}
