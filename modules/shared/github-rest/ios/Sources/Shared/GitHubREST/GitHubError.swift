import Foundation

public enum GitHubError: Error, Equatable, Sendable {
    case notFound
    case invalidQuery
    case forbidden
    case rateLimited(until: Date)
    case server(Int)
}
