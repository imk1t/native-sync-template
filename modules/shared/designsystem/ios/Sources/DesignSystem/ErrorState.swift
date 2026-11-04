import SwiftUI

public struct ErrorState: View {
    let message: String
    let retry: () -> Void
    public init(_ message: String, retry: @escaping () -> Void) {
        self.message = message
        self.retry = retry
    }
    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(message, systemImage: "exclamationmark.triangle").foregroundStyle(.secondary)
            Button("再試行", action: retry).buttonStyle(.bordered)
        }.padding(.vertical, 8)
    }
}
