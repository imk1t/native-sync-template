# ADR 0007: 汎用 Extension と専用のユーティリティ型を Core に分ける

- 状態: 採用

## 決定
- `core/extensions` は標準型の汎用拡張を提供する。初期実装は StringExtensions に空白判定・前後の空白除去・Unicode コードポイント数を集約する。
- `core/utils` は専用の型で提供する汎用処理を置き、初期実装は DateTimeUtils の時刻整形とする。ロケールとタイムゾーンは表示時に呼び出し側が渡す。
- `core/extensions` の内部依存は禁止し、`core/utils` の内部依存は `core/extensions` のみ許可する。両者は外部ライブラリに依存しない。標準ライブラリ・Foundation・JDK とテスト用依存は利用できる。
- `shared/designsystem` はこの2モジュールのみ参照可能とする。依存は利用する場合だけ追加し、今回は追加しない。
- メモの長さの業務ルールは `shared/bookmarks` の BookmarkNoteRules に置き、表示と保存で共用する。

## Extension の扱い
値の参照・判定はプロパティ、変換は関数とする。標準 API で仕様を満たせる場合はそのまま利用し、OS 間の契約統一や重複解消に必要な拡張だけを追加する。同じ処理を Utils と Extension の両方で公開しない。

Message.localized は core/localization、Route の URL 変換は core/navigation、画面固有の変換は feature に残す。Extension 構文を使っているという理由だけで CoreExtensions に集めない。

## 理由と影響
Shared は複数 feature で共有する UI・業務モデル・処理の層で、shared 同士は依存しない。保存時にも必要な汎用の文字列操作を Core に置くことで、この依存方向を維持できる。独立した CoreExtensions により汎用拡張の公開場所を明確にする。

空白判定と trim は OS 標準の差に依存せず、core/extensions の spec で定めた同じ Unicode 空白集合を使う。メモの上限と保存形式は維持する。数値を含む文言・複数形は既存の Message を利用し、NumberUtils は追加しない。Issue 更新日時の解析・表示改善は今回の対象外とする。

依存制約は AGENTS.md と tools/check-deps.ts に反映し、両 OS のビルド定義と検査を揃える。
