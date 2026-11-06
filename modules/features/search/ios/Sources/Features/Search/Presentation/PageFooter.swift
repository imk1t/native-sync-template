import CoreLocalization
import DesignSystem
import SwiftUI

struct PageFooter<Value: Equatable & Sendable>: View {
    @Environment(\.locale) private var locale
    let state: PagedState<Value>
    let retry: () -> Void
    let next: () -> Void

    var body: some View {
        Group {
            if state.isLoading { LoadingIndicator(Message.commonLoading.resolve(locale: locale)) }
            if let error = state.error {
                ErrorState(
                    error.resolve(locale: locale),
                    retryLabel: Message.commonRetry.resolve(locale: locale),
                    retry: retry)
            }
            if state.hasLoaded && state.items.isEmpty && !state.isLoading {
                EmptyState(
                    Message.searchEmptyTitle.resolve(locale: locale),
                    message: Message.searchEmptyMessage.resolve(locale: locale))
            }
            if state.incomplete { Text(Message.searchIncomplete.resolve(locale: locale)).font(.footnote) }
            if state.limitReached { Text(Message.searchLimit.resolve(locale: locale)).font(.footnote) }
            if state.nextPage != nil {
                Button(Message.searchMore.resolve(locale: locale), action: next).disabled(state.isLoading).frame(
                    maxWidth: .infinity)
            }
        }.designListRow()
    }
}
