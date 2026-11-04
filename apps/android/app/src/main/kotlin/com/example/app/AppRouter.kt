package com.example.app

import androidx.compose.runtime.Composable
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import com.example.featurebookmarks.BookmarkNoteScreen
import com.example.featurebookmarks.BookmarksScreen
import com.example.featuresearch.IssueDetailScreen
import com.example.featuresearch.RepositoryDetailScreen
import com.example.featuresearch.RepositoryIssuesScreen
import com.example.featuresearch.RepositorySearchScreen
import com.example.featuresettings.SettingsScreen

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
