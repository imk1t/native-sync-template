import SwiftUI

public struct ErrorState: View {
    let message: String
    let retryLabel: String
    let retry: () -> Void
    public init(_ message: String, retryLabel: String, retry: @escaping () -> Void) {
        self.message = message
        self.retryLabel = retryLabel
        self.retry = retry
    }
    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(message, systemImage: "exclamationmark.triangle").foregroundStyle(.secondary)
            Button(retryLabel, action: retry).buttonStyle(.bordered)
        }.padding(.vertical, 8)
    }
}
