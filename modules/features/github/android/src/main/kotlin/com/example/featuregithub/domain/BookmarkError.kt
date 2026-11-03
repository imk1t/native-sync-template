package com.example.featuregithub

sealed class BookmarkError(message: String) : Exception(message) {
    class Limit : BookmarkError("保存できるのは100件までです。")
    class NoteTooLong : BookmarkError("メモは1,000文字以内で入力してください。")
    class NotFound : BookmarkError("このブックマークは削除されています。")
    class UnsupportedVersion : BookmarkError("保存データを読み込めません。アプリのバージョンを確認してください。")
}
