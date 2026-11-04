package com.example.shared.designsystem

/** リポジトリ行の表示値。API のモデルや保存処理には依存しない。 */
data class RepositoryRowData(
    val fullName: String,
    val owner: String,
    val avatarURL: String? = null,
    val description: String? = null,
    val language: String? = null,
    val stars: Int = 0
)
