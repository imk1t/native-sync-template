import SwiftUI

private struct DesignFontModifier: ViewModifier {
    @ScaledMetric private var size: CGFloat
    private let weight: Font.Weight
    private let monospaced: Bool

    init(style: DesignTextStyle, relativeTo textStyle: Font.TextStyle, monospaced: Bool) {
        _size = ScaledMetric(wrappedValue: style.size, relativeTo: textStyle)
        weight = style.weight
        self.monospaced = monospaced
    }

    func body(content: Content) -> some View {
        content.font(.system(size: size, weight: weight, design: monospaced ? .monospaced : .default))
    }
}

public extension View {
    /// 文字サイズと太さを Tokens の文字スタイルにする。Dynamic Type には `textStyle` を基準に追従する。
    func designFont(
        _ style: DesignTextStyle, relativeTo textStyle: Font.TextStyle = .body, monospaced: Bool = false
    ) -> some View {
        modifier(DesignFontModifier(style: style, relativeTo: textStyle, monospaced: monospaced))
    }

    /// 補足の文字（caption）。件数・日付・見出しラベルなどに使う。
    func designCaption() -> some View {
        designFont(Tokens.Typography.caption, relativeTo: .footnote)
    }
}
