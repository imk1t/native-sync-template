import SwiftUI

public extension View {
    /// List / Form の背景を Tokens の background にする。
    func designScreenBackground() -> some View {
        scrollContentBackground(.hidden).background(Tokens.Color.background)
    }

    /// List の行を、行の区切り線と背景をなくして余白だけにする（中身は `designCard()` で囲む）。
    func designListRow() -> some View {
        listRowBackground(Color.clear).listRowSeparator(.hidden).listRowInsets(
            EdgeInsets(
                top: Tokens.Spacing.small / 2, leading: Tokens.Spacing.medium,
                bottom: Tokens.Spacing.small / 2, trailing: Tokens.Spacing.medium))
    }

    /// 面（surface）と細い線で囲んだカード。
    func designCard() -> some View {
        padding(Tokens.Spacing.medium)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Tokens.Color.surface, in: RoundedRectangle(cornerRadius: Tokens.Radius.card))
            .overlay(RoundedRectangle(cornerRadius: Tokens.Radius.card).stroke(Tokens.Color.line))
    }
}
