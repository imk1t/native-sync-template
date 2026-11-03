package com.example.corenetwork

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class GitHubError(message: String) : Exception(message) {
    class NotFound : GitHubError("見つかりませんでした。削除または公開設定が変更された可能性があります。")
    class InvalidQuery : GitHubError("検索条件を確認してください。")
    class Forbidden : GitHubError("この情報へのアクセスが許可されていません。")
    class RateLimited(val until: Long) :
        GitHubError(
            "取得回数の上限です。${DateTimeFormatter.ofPattern(
                "HH:mm:ss"
            ).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(until))}以降に再試行してください。"
        )
    class Server(val status: Int) : GitHubError("取得に失敗しました（HTTP $status）。時間をおいて再試行してください。")
}
