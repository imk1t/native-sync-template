import CoreNetwork

public struct PagedState<Value: Equatable & Sendable>: Equatable {
    public var items: [Value] = []
    public var hasLoaded = false
    public var isLoading = false
    public var error: String?
    public var nextPage: Int?
    public var incomplete = false
    public var limitReached = false
}
