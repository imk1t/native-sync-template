import SwiftUI

public extension View {
    /// アプリ全体の配色と本文の文字サイズ。アクセント色を Tokens に揃える（背景・文字色は OS の設定に従う）。
    func designTheme() -> some View {
        tint(Tokens.Color.primary).designFont(Tokens.Typography.body)
    }
}
