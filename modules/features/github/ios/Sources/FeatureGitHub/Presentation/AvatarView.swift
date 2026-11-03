import CoreNetwork
import SwiftUI

struct AvatarView: View {
    let url: String?
    let loader: ImageLoader
    @State private var data: Data?

    var body: some View {
        Group {
            if let data, let image = UIImage(data: data) {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                Image(systemName: "person.crop.circle.fill").resizable().foregroundStyle(.secondary)
            }
        }
        .frame(width: 40, height: 40).clipShape(RoundedRectangle(cornerRadius: 10))
        .accessibilityHidden(true)
        .task(id: url) {
            data = nil
            if let url { data = try? await loader.load(url) }
        }
    }
}
