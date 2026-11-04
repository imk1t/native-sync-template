import Foundation

public enum GitHubError: Error, Equatable, LocalizedError, Sendable {
    case notFound
    case invalidQuery
    case forbidden
    case rateLimited(until: Date)
    case server(Int)

    public var errorDescription: String? {
        switch self {
        case .notFound: "見つかりませんでした。削除または公開設定が変更された可能性があります。"
        case .invalidQuery: "検索条件を確認してください。"
        case .forbidden: "この情報へのアクセスが許可されていません。"
        case .rateLimited(let date): "取得回数の上限です。\(date.formatted(date: .omitted, time: .standard))以降に再試行してください。"
        case .server(let status): "取得に失敗しました（HTTP \(status)）。時間をおいて再試行してください。"
        }
    }
}
