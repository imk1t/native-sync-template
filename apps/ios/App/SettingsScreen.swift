import CoreNavigation
import FeatureGitHub
import FeatureSettings
import SwiftUI

struct SettingsScreen: View {
    @State private var viewModel: SettingsViewModel
    init(dependencies: GitHubDependencies) {
        _viewModel = State(initialValue: SettingsViewModel(settings: dependencies.settings))
    }
    var body: some View { SettingsView(viewModel: viewModel) }
}
