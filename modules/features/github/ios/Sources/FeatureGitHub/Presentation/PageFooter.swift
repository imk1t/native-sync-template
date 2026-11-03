import DesignSystem
import SwiftUI

struct PageFooter<Value: Equatable & Sendable>: View {
    let state: PagedState<Value>
    let retry: () -> Void
    let next: () -> Void

    var body: some View {
        if state.isLoading { LoadingIndicator() }
        if let error = state.error { ErrorState(error, retry: retry) }
        if state.hasLoaded && state.items.isEmpty && !state.isLoading {
            EmptyState("結果がありません", message: "条件を変えて探すか、続きがある場合は次のページを確認してください。")
        }
        if state.incomplete { Text("検索結果の一部を取得できませんでした。条件を絞って再検索できます。").font(.footnote) }
        if state.limitReached { Text("検索結果は最大1,000件まで表示します。").font(.footnote) }
        if state.nextPage != nil {
            Button("さらに読み込む", action: next).disabled(state.isLoading).frame(maxWidth: .infinity)
        }
    }
}
