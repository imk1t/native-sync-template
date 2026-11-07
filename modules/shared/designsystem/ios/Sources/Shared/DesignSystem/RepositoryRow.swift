import SwiftUI

private enum MetaKind { case language, stars, saved }

private struct MetaItem: Identifiable {
    let kind: MetaKind
    let text: String
    var id: MetaKind { kind }
}

public struct RepositoryRow: View {
    let repository: RepositoryRowData
    let saved: Bool
    let showDescription: Bool
    let starsLabel: String
    let savedLabel: String
    let loadImage: (String) async throws -> Data
    @ScaledMetric(relativeTo: .caption) private var dotSize: CGFloat = 8

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
                FlowLayout {
                    ForEach(metaItems) { item in metaView(item) }
                }.font(.caption).foregroundStyle(Tokens.Color.muted)
            }.frame(maxWidth: .infinity, alignment: .leading)
        }.accessibilityElement(children: .combine)
    }

    private var metaItems: [MetaItem] {
        var items: [MetaItem] = []
        if let language = repository.language { items.append(MetaItem(kind: .language, text: language)) }
        items.append(MetaItem(kind: .stars, text: starsLabel))
        if saved { items.append(MetaItem(kind: .saved, text: savedLabel)) }
        return items
    }

    @ViewBuilder private func metaView(_ item: MetaItem) -> some View {
        HStack(spacing: 4) {
            switch item.kind {
            case .language:
                Circle().fill(languageTint(item.text)).frame(width: dotSize, height: dotSize)
            case .stars:
                Image(systemName: "star")
            case .saved:
                Image(systemName: "bookmark.fill")
            }
            Text(item.text)
        }
        .fixedSize(horizontal: false, vertical: true)
        .foregroundStyle(item.kind == .saved ? Tokens.Color.primary : Tokens.Color.muted)
    }
}
