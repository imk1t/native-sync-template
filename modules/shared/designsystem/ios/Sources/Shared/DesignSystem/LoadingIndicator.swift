import SwiftUI

public struct LoadingIndicator: View {
    let label: String
    public init(_ label: String) { self.label = label }
    public var body: some View { ProgressView(label).frame(maxWidth: .infinity).padding() }
}
