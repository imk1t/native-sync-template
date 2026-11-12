import CoreLocalization
import CoreUtils
import Foundation
import SharedGitHub

public enum SearchFailure: Equatable, Sendable {
    case message(Message)
    case rateLimited(until: Date)

    init(_ error: Error) {
        switch error as? GitHubError {
        case .notFound: self = .message(.errorNotFound)
        case .invalidQuery: self = .message(.errorInvalidQuery)
        case .forbidden: self = .message(.errorForbidden)
        case .rateLimited(let until): self = .rateLimited(until: until)
        case .server(let status): self = .message(.errorServer(status: status))
        case nil: self = .message(.errorNetwork)
        }
    }

    func resolve(locale: Locale) -> String {
        switch self {
        case .message(let message): message.resolve(locale: locale)
        case .rateLimited(let until):
            Message.errorRateLimited(time: DateTimeUtils.formatTime(until, locale: locale, timeZone: .current))
                .resolve(locale: locale)
        }
    }
}

func issueStateLabel(_ state: String, locale: Locale) -> String {
    switch state {
    case "open": Message.issueOpen.resolve(locale: locale)
    case "closed": Message.issueClosed.resolve(locale: locale)
    default: state
    }
}
