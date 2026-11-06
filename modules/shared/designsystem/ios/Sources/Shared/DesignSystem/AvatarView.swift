import SwiftUI

struct AvatarView: View {
    let url: String?
    let loadImage: (String) async throws -> Data
    @State private var data: Data?

    var body: some View {
        Group {
            if let data, let image = UIImage(data: data) {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                Image(systemName: "person.crop.circle.fill").resizable().foregroundStyle(Tokens.Color.muted)
            }
        }
        .frame(width: 44, height: 44).clipShape(RoundedRectangle(cornerRadius: Tokens.Radius.avatar))
        .accessibilityHidden(true)
        .task(id: url) {
            data = nil
            if let url { data = try? await loadImage(url) }
        }
    }
}
