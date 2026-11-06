import SwiftUI

public struct StatusBadge: View {
    let text: String
    let emphasized: Bool
    public init(_ text: String, emphasized: Bool = false) {
        self.text = text
        self.emphasized = emphasized
    }
    public var body: some View {
        Text(text).font(.caption).bold().padding(.horizontal, 10).padding(.vertical, 5)
            .foregroundStyle(emphasized ? Tokens.Color.primary : Tokens.Color.muted)
            .background((emphasized ? Tokens.Color.primary : Tokens.Color.muted).opacity(0.12), in: Capsule())
    }
}
