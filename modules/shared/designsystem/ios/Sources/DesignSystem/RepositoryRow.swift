import SwiftUI

public struct RepositoryRow: View {
    let repository: RepositoryRowData
    let saved: Bool
    let showDescription: Bool
    let starsLabel: String
    let savedLabel: String
    let loadImage: (String) async throws -> Data

    public init(
        repository: RepositoryRowData, saved: Bool, showDescription: Bool,
        starsLabel: String, savedLabel: String, loadImage: @escaping (String) async throws -> Data
    ) {
        self.repository = repository
        self.saved = saved
        self.showDescription = showDescription
        self.starsLabel = starsLabel
        self.savedLabel = savedLabel
        self.loadImage = loadImage
    }

    public var body: some View {
        HStack(alignment: .top, spacing: 12) {
            AvatarView(url: repository.avatarURL, loadImage: loadImage)
            VStack(alignment: .leading, spacing: 6) {
                Text(repository.fullName).font(.headline).foregroundStyle(.primary)
                if showDescription, let description = repository.description, !description.isEmpty {
                    Text(description).font(.subheadline).foregroundStyle(.secondary).lineLimit(3)
                }
                HStack {
                    Label(starsLabel, systemImage: "star")
                    if let language = repository.language { Text(language) }
                    if saved { Label(savedLabel, systemImage: "bookmark.fill") }
                }.font(.caption).foregroundStyle(.secondary)
            }
        }.padding(.vertical, 6).accessibilityElement(children: .combine)
    }
}
