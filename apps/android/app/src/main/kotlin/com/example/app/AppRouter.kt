package com.example.app

import androidx.compose.runtime.Composable
import com.example.corenavigation.Navigator
import com.example.corenavigation.Route
import com.example.featuregithub.BookmarkNoteScreen
import com.example.featuregithub.BookmarksScreen
import com.example.featuregithub.IssueDetailScreen
import com.example.featuregithub.RepositoryDetailScreen
import com.example.featuregithub.RepositoryIssuesScreen
import com.example.featuregithub.RepositorySearchScreen
import com.example.featuresettings.SettingsScreen

@Composable
internal fun AppRouter(route: Route, dependencies: AppDependencies, navigator: Navigator) {
    when (route) {
        Route.RepositorySearch -> RepositorySearchScreen(dependencies.github, navigator)
        is Route.RepositoryDetail -> RepositoryDetailScreen(route.owner, route.repo, dependencies.github, navigator)
        is Route.RepositoryIssues -> RepositoryIssuesScreen(route.owner, route.repo, dependencies.github, navigator)
        is Route.IssueDetail -> IssueDetailScreen(route.owner, route.repo, route.number, dependencies.github)
        Route.Bookmarks -> BookmarksScreen(dependencies.github, navigator)
        is Route.BookmarkNote -> BookmarkNoteScreen(route.repositoryId, dependencies.github, navigator)
        Route.Settings -> SettingsScreen(dependencies.settings)
    }
}
