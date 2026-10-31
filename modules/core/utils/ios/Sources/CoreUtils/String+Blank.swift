public extension String {
    var isBlankOrEmpty: Bool { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
}
