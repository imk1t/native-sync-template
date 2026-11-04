import CoreNavigation
import FeatureBookmarks
import FeatureSearch
import FeatureSettings
import SwiftUI

struct AppRouter: View {
    let route: Route
    let dependencies: AppDependencies
    let navigator: Navigator

    var body: some View {
        switch route {
        case .repositorySearch: RepositorySearchScreen(dependencies: dependencies.search, navigator: navigator)
        case .repositoryDetail(let owner, let repo):
            RepositoryDetailScreen(owner: owner, repo: repo, dependencies: dependencies.search, navigator: navigator)
        case .repositoryIssues(let owner, let repo):
            RepositoryIssuesScreen(owner: owner, repo: repo, dependencies: dependencies.search, navigator: navigator)
        case .issueDetail(let owner, let repo, let number):
            IssueDetailScreen(owner: owner, repo: repo, number: number, dependencies: dependencies.search)
        case .bookmarks: BookmarksScreen(dependencies: dependencies.bookmarks, navigator: navigator)
        case .bookmarkNote(let repositoryId):
            BookmarkNoteScreen(repositoryId: repositoryId, dependencies: dependencies.bookmarks, navigator: navigator)
        case .settings: SettingsScreen(settings: dependencies.settings)
        }
    }
}
