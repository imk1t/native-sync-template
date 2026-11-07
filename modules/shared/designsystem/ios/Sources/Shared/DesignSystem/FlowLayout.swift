import SwiftUI

/// 子ビューを横に並べ、幅に収まらなければ次の行へ折り返す。
struct FlowLayout: Layout {
    var spacing: CGFloat = 10
    var lineSpacing: CGFloat = 4

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        arrange(width: proposal.width ?? .infinity, subviews: subviews).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let origins = arrange(width: bounds.width, subviews: subviews).origins
        for (index, origin) in origins.enumerated() {
            let ideal = subviews[index].sizeThatFits(.unspecified).width
            subviews[index].place(
                at: CGPoint(x: bounds.minX + origin.x, y: bounds.minY + origin.y),
                proposal: ProposedViewSize(width: min(ideal, bounds.width), height: nil))
        }
    }

    private func arrange(width: CGFloat, subviews: Subviews) -> (size: CGSize, origins: [CGPoint]) {
        var posX: CGFloat = 0
        var posY: CGFloat = 0
        var rowHeight: CGFloat = 0
        var usedWidth: CGFloat = 0
        var origins: [CGPoint] = []
        for subview in subviews {
            var size = subview.sizeThatFits(.unspecified)
            if size.width > width { size = subview.sizeThatFits(ProposedViewSize(width: width, height: nil)) }
            if posX > 0, posX + size.width > width {
                posX = 0
                posY += rowHeight + lineSpacing
                rowHeight = 0
            }
            origins.append(CGPoint(x: posX, y: posY))
            posX += size.width
            usedWidth = max(usedWidth, posX)
            posX += spacing
            rowHeight = max(rowHeight, size.height)
        }
        return (CGSize(width: usedWidth, height: posY + rowHeight), origins)
    }
}
