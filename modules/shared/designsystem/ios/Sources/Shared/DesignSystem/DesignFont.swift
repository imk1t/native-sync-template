import CoreFont
import SwiftUI

private struct DesignFontModifier: ViewModifier {
    @ScaledMetric private var size: CGFloat
    private let family: String
    private let weightValue: Int
    private let monospaced: Bool

    init(style: DesignTextStyle, relativeTo textStyle: Font.TextStyle, monospaced: Bool, weightValue: Int?) {
        _size = ScaledMetric(wrappedValue: style.size, relativeTo: textStyle)
        family = style.family
        self.weightValue = weightValue ?? style.weightValue
        self.monospaced = monospaced
    }

    func body(content: Content) -> some View {
        // 同梱の書体があればそれを使い、ない太さ・書体名と日本語の字は OS の書体にする。
        if let name = FontRegistry.postScriptName(family: family, weight: weightValue),
            FontRegistry.isAvailable(postScriptName: name) {
            content.font(.custom(name, fixedSize: size))
        } else {
            content.font(
                .system(size: size, weight: Self.weight(weightValue), design: monospaced ? .monospaced : .default))
        }
    }

    private static func weight(_ value: Int) -> Font.Weight {
        switch value {
        case ..<150: .ultraLight
        case ..<250: .thin
        case ..<350: .light
        case ..<450: .regular
        case ..<550: .medium
        case ..<650: .semibold
        case ..<750: .bold
        case ..<850: .heavy
        default: .black
        }
    }
}

public extension View {
    /// 書体・文字サイズ・太さを Tokens の文字スタイルにする。Dynamic Type には `textStyle` を基準に追従する。
    /// `weightValue` で太さだけ変えられる（同梱の書体にない太さは OS の書体になる）。
    func designFont(
        _ style: DesignTextStyle, relativeTo textStyle: Font.TextStyle = .body, monospaced: Bool = false,
        weightValue: Int? = nil
    ) -> some View {
        modifier(
            DesignFontModifier(style: style, relativeTo: textStyle, monospaced: monospaced, weightValue: weightValue))
    }

    /// 本文（body）の書体で、`textStyle` に追従する。`weightValue` で太さだけ変えられる。
    func designBody(relativeTo textStyle: Font.TextStyle = .body, weightValue: Int? = nil) -> some View {
        designFont(Tokens.Typography.body, relativeTo: textStyle, weightValue: weightValue)
    }

    /// 補足の文字（caption）。件数・日付・見出しラベルなどに使う。
    func designCaption() -> some View {
        designFont(Tokens.Typography.caption, relativeTo: .footnote)
    }
}
