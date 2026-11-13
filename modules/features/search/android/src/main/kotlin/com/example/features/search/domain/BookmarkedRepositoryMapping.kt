package com.example.features.search

import com.example.shared.bookmarks.BookmarkedRepository
import com.example.shared.githubrest.RepositorySummary

/** 検索結果のリポジトリを、保存用のスナップショットに変換する。 */
fun RepositorySummary.toBookmarked() = BookmarkedRepository(
    id, name, fullName, owner, avatarURL, description, language, stars, forks, topics, htmlURL
)
