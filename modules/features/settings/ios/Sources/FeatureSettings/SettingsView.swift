import DesignSystem
import SwiftUI

public struct SettingsView: View {
    @Bindable private var viewModel: SettingsViewModel
    public init(viewModel: SettingsViewModel) { self.viewModel = viewModel }
    public var body: some View {
        Form {
            Section("表示") {
                Toggle("一覧に説明文を表示", isOn: $viewModel.showDescriptions)
                    .disabled(!viewModel.isLoaded || viewModel.isSaving)
                    .onChange(of: viewModel.showDescriptions) { Task { await viewModel.save() } }
            }
            if viewModel.isSaving { LoadingIndicator() }
            if let error = viewModel.error { ErrorState(error) { Task { await viewModel.load() } } }
            Section("このアプリについて") {
                Text("GitHub Explorer").font(.headline)
                Text("公開リポジトリを探して保存する、iOS・Android のネイティブ開発サンプルです。")
                Text("ブックマークとメモはこの端末に保存されます。GitHub の Star とは連動しません。")
                    .font(.footnote).foregroundStyle(.secondary)
            }
        }
        .navigationTitle("設定")
        .task { await viewModel.load() }
    }
}
