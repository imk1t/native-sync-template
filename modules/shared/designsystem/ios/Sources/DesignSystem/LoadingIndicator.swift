import SwiftUI

public struct LoadingIndicator: View {
    public init() {}
    public var body: some View { ProgressView("読み込み中").frame(maxWidth: .infinity).padding() }
}
