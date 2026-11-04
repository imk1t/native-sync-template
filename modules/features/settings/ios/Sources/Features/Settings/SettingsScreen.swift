import CoreStorage
import SwiftUI

public struct SettingsScreen: View {
    @State private var viewModel: SettingsViewModel
    public init(settings: DisplaySettings) {
        _viewModel = State(initialValue: SettingsViewModel(settings: settings))
    }
    public var body: some View { SettingsView(viewModel: viewModel) }
}
