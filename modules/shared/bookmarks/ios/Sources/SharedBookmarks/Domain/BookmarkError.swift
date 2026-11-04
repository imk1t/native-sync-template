import Foundation

public enum BookmarkError: Error, LocalizedError {
    case limit
    case noteTooLong
    case notFound
    case unsupportedVersion

    public var errorDescription: String? {
        switch self {
        case .limit: "保存できるのは100件までです。"
        case .noteTooLong: "メモは1,000文字以内で入力してください。"
        case .notFound: "このブックマークは削除されています。"
        case .unsupportedVersion: "保存データを読み込めません。アプリのバージョンを確認してください。"
        }
    }
}
