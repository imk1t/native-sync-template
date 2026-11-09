import CoreText
import Foundation
import UIKit

/// `DESIGN.md` の書体をアプリに同梱して使うための登録。ファイルと書体の対応は spec.md に書く。
public enum FontRegistry {
    private struct Face {
        let family: String
        let weight: Int
        let postScriptName: String
    }

    private static let faces = [
        Face(family: "Inter Tight", weight: 400, postScriptName: "InterTight-Regular"),
        Face(family: "Inter Tight", weight: 500, postScriptName: "InterTight-Medium"),
        Face(family: "Inter Tight", weight: 700, postScriptName: "InterTight-Bold"),
        Face(family: "JetBrains Mono", weight: 500, postScriptName: "JetBrainsMono-Medium")
    ]

    /// 同梱の書体ファイルをプロセスに登録する。何度呼んでも、登録済みでも失敗にしない。
    public static func registerAll() {
        _ = available
    }

    /// 登録して、実際に作れた書体の PostScript 名。最初の 1 回だけ登録する。
    private static let available: Set<String> = {
        var names = Set<String>()
        for face in faces {
            guard let url = Bundle.module.url(forResource: face.postScriptName, withExtension: "ttf") else { continue }
            var error: Unmanaged<CFError>?
            // 登録済みのエラーは無視する（プレビューなどで別の経路から登録されることがある）。
            _ = CTFontManagerRegisterFontsForURL(url as CFURL, .process, &error)
            error?.release()
            if UIFont(name: face.postScriptName, size: 12) != nil { names.insert(face.postScriptName) }
        }
        return names
    }()

    /// 書体名（DESIGN.md の fontFamily）と太さ（100〜900）に対応する同梱の PostScript 名。ない場合は nil。
    public static func postScriptName(family: String, weight: Int) -> String? {
        faces.first { $0.family == family && $0.weight == weight }?.postScriptName
    }

    /// 登録されていて、`UIFont(name:size:)` で作れる書体か。
    public static func isAvailable(postScriptName: String) -> Bool {
        available.contains(postScriptName)
    }
}
