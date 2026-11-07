import CoreLocalization
import DesignSystem
import SwiftUI

struct SettingsView: View {
    @Environment(\.locale) private var locale
    @Bindable private var viewModel: SettingsViewModel
    init(viewModel: SettingsViewModel) { self.viewModel = viewModel }
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Tokens.Spacing.medium) {
                Text(Message.settingsDisplay.resolve(locale: locale)).designCaption()
                    .foregroundStyle(Tokens.Color.muted)
                Toggle(Message.settingsDescriptions.resolve(locale: locale), isOn: $viewModel.showDescriptions)
                    .disabled(!viewModel.isLoaded || viewModel.isSaving)
                    .onChange(of: viewModel.showDescriptions) { Task { await viewModel.save() } }
                    .designCard()
                if viewModel.isSaving { LoadingIndicator(Message.commonLoading.resolve(locale: locale)) }
                if let error = viewModel.error {
                    ErrorState(error.resolve(locale: locale), retryLabel: Message.commonRetry.resolve(locale: locale)) {
                        Task { await viewModel.load() }
                    }
                }
                Text(Message.settingsAbout.resolve(locale: locale)).designCaption()
                    .foregroundStyle(Tokens.Color.muted)
                VStack(alignment: .leading, spacing: 8) {
                    Text(Message.appName.resolve(locale: locale)).font(.headline)
                    Text(Message.settingsDescription.resolve(locale: locale))
                    Text(Message.settingsStorageNote.resolve(locale: locale))
                        .designCaption().foregroundStyle(Tokens.Color.muted)
                }.designCard()
            }.padding(Tokens.Spacing.medium)
        }
        .background(Tokens.Color.background)
        .navigationTitle(Message.settingsTitle.resolve(locale: locale))
        .task { await viewModel.load() }
    }
}
