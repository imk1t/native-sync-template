public extension String {
    var isBlankOrEmpty: Bool { allSatisfy { $0.isWhitespace } }
}
