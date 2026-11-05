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
                Text(repository.fullName)
                    .font(.system(.subheadline, design: .monospaced).weight(.medium))
                    .foregroundStyle(Tokens.Color.onBackground)
                if showDescription, let description = repository.description, !description.isEmpty {
                    Text(description).font(.subheadline).foregroundStyle(Tokens.Color.muted).lineLimit(3)
                }
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 10) { meta }
                    VStack(alignment: .leading, spacing: 4) { meta }
                }.font(.caption).foregroundStyle(Tokens.Color.muted)
            }
            Spacer(minLength: 0)
        }.accessibilityElement(children: .combine)
    }

    @ViewBuilder private var meta: some View {
        if let language = repository.language {
            Label {
                Text(language).lineLimit(1)
            } icon: {
                Circle().fill(languageTint(language)).frame(width: 8, height: 8)
            }
        }
        Label(starsLabel, systemImage: "star").lineLimit(1)
        if saved {
            Label(savedLabel, systemImage: "bookmark.fill").lineLimit(1).foregroundStyle(Tokens.Color.primary)
        }
    }
}
