import DesignSystem
import SwiftUI

public struct BookmarkNoteView: View {
    @Bindable private var viewModel: BookmarkNoteViewModel
    public init(viewModel: BookmarkNoteViewModel) { self.viewModel = viewModel }
    public var body: some View {
        Form {
            Section("メモ") {
                TextField("気になったことを書いておきましょう", text: $viewModel.note, axis: .vertical)
                    .lineLimit(6...12).disabled(!viewModel.isLoaded || viewModel.isSaving)
                Text("\(viewModel.characterCount) / 1,000文字")
                    .foregroundStyle(viewModel.characterCount > 1_000 ? .red : .secondary)
                    .font(.caption)
            }
            if let error = viewModel.error {
                if viewModel.isLoaded {
                    Text(error).foregroundStyle(.red)
                } else {
                    ErrorState(error) { Task { await viewModel.load() } }
                }
            }
            if viewModel.isSaving { LoadingIndicator() }
        }
        .navigationTitle("メモを編集").navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("キャンセル", action: viewModel.cancel).disabled(viewModel.isSaving)
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("保存") { Task { await viewModel.save() } }.disabled(!viewModel.canSave)
            }
        }
        .interactiveDismissDisabled(viewModel.isSaving)
        .task { await viewModel.load() }
    }
}
