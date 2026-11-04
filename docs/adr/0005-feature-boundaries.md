# ADR 0005: タブごとの feature と共通実装・仕様の境界

## 決定
- 画面は `features/search`（探す・リポジトリ詳細・Issue）、`features/bookmarks`（保存済み・メモ）、`features/settings`（設定）に分ける。
- `modules/shared/*` は複数 feature で使うモデルと処理を置く層とする。依存先は core のみ。features は shared・core・designsystem を使え、features 同士は依存しない。
- `shared/bookmarks` の BookmarkRepository が core/storage の KeyValueStore を包み、保存 Key・JSON・更新の直列化・状態通知を担当する。画面・ViewModel・API 通信は持たない。
- AppDependencies で一度生成した BookmarkRepository・DisplaySettings・ImageLoader を各 feature の依存コンテナへ渡す。保存済みから詳細を開くときは Route と AppRouter を経由する。
- 共通の RepositoryRow は designsystem に置き、API モデルの代わりに RepositoryRowData と画像取得処理を受け取る。
- ルートの `shared/` は `specs/` に改称する。OpenAPI・tokens・routes・scenarios・third-party の仕様・生成元と、`modules/shared/` の共通実装を区別する。

## 理由と影響
Key を渡して各 feature が直接保存すると、JSON 形式・同時更新・変更通知が分散する。共通ラッパーにまとめることで core/storage を汎用の保存処理に保ち、検索と保存済みの状態を揃える。

保存 Key `github.bookmarks.v1` と JSON version 1 の形式、Route・ディープリンク・画面の振る舞いは維持する。生成・検査ツールは specs を参照し、shared 層の依存方向を両 OS で検査する。各モジュールの振る舞いは引き続き `modules/**/spec.md` を正とする。
