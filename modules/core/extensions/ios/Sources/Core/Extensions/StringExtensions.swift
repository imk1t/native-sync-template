public extension String {
    var isBlankOrEmpty: Bool { unicodeScalars.allSatisfy(\.isUtilityWhitespace) }

    var codePointCount: Int { unicodeScalars.count }

    func trimmedWhitespace() -> String {
        let scalars = unicodeScalars
        guard let start = scalars.firstIndex(where: { !$0.isUtilityWhitespace }),
            let end = scalars.lastIndex(where: { !$0.isUtilityWhitespace })
        else { return "" }
        return String(scalars[start...end])
    }
}

private extension Unicode.Scalar {
    var isUtilityWhitespace: Bool {
        switch value {
        case 0x0009...0x000D, 0x0020, 0x0085, 0x00A0, 0x1680, 0x2000...0x200A,
            0x2028, 0x2029, 0x202F, 0x205F, 0x3000:
            true
        default: false
        }
    }
}
