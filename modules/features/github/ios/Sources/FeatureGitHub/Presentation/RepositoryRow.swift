import CoreNetwork
import SwiftUI

struct RepositoryRow: View {
    let repository: RepositorySummary
    let saved: Bool
    let showDescription: Bool
    let images: ImageLoader

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            AvatarView(url: repository.avatarURL, loader: images)
            VStack(alignment: .leading, spacing: 6) {
                Text(repository.fullName).font(.headline).foregroundStyle(.primary)
                if showDescription, let description = repository.description, !description.isEmpty {
                    Text(description).font(.subheadline).foregroundStyle(.secondary).lineLimit(3)
                }
                HStack {
                    Label(repository.stars.formatted(), systemImage: "star")
                    if let language = repository.language { Text(language) }
                    if saved { Label("保存済み", systemImage: "bookmark.fill") }
                }.font(.caption).foregroundStyle(.secondary)
            }
        }.padding(.vertical, 6).accessibilityElement(children: .combine)
    }
}
