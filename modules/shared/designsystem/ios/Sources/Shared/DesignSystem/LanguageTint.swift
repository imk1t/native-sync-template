import SwiftUI

/// プログラミング言語を示す点の色。未知の言語は muted。
public func languageTint(_ language: String?) -> Color {
    switch language {
    case "Swift": Color(hex: 0xE8452C)
    case "Kotlin": Color(hex: 0x7F52FF)
    case "TypeScript": Color(hex: 0x3178C6)
    case "Rust": Color(hex: 0xB7410E)
    default: Tokens.Color.muted
    }
}
