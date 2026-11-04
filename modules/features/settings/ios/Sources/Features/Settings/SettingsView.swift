import CoreLocalization
import DesignSystem
import SwiftUI

struct SettingsView: View {
    @Environment(\.locale) private var locale
    @Bindable private var viewModel: SettingsViewModel
    init(viewModel: SettingsViewModel) { self.viewModel = viewModel }
    var body: some View {
        Form {
            Section(Message.settingsDisplay.resolve(locale: locale)) {
                Toggle(Message.settingsDescriptions.resolve(locale: locale), isOn: $viewModel.showDescriptions)
                    .disabled(!viewModel.isLoaded || viewModel.isSaving)
                    .onChange(of: viewModel.showDescriptions) { Task { await viewModel.save() } }
            }
            if viewModel.isSaving { LoadingIndicator(Message.commonLoading.resolve(locale: locale)) }
            if let error = viewModel.error {
                ErrorState(error.resolve(locale: locale), retryLabel: Message.commonRetry.resolve(locale: locale)) {
                    Task { await viewModel.load() }
                }
            }
            Section(Message.settingsAbout.resolve(locale: locale)) {
                Text(Message.appName.resolve(locale: locale)).font(.headline)
                Text(Message.settingsDescription.resolve(locale: locale))
                Text(Message.settingsStorageNote.resolve(locale: locale))
                    .font(.footnote).foregroundStyle(.secondary)
            }
        }
        .navigationTitle(Message.settingsTitle.resolve(locale: locale))
        .task { await viewModel.load() }
    }
}
