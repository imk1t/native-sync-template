public struct Page<Value: Equatable & Sendable>: Equatable, Sendable {
    public let items: [Value]
    public let nextPage: Int?
    public let incomplete: Bool
    public let limitReached: Bool

    public init(items: [Value], nextPage: Int? = nil, incomplete: Bool = false, limitReached: Bool = false) {
        self.items = items
        self.nextPage = nextPage
        self.incomplete = incomplete
        self.limitReached = limitReached
    }
}
