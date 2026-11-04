import SwiftUI

public struct EmptyState: View {
    let title: String
    let message: String
    let symbol: String
    public init(_ title: String, message: String, symbol: String = "tray") {
        self.title = title
        self.message = message
        self.symbol = symbol
    }
    public var body: some View {
        ContentUnavailableView(title, systemImage: symbol, description: Text(message))
    }
}
