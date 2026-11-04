# shared/bookmarks

## 概要
複数 feature で使うブックマークのモデルと BookmarkRepository を提供する。core/storage の KeyValueStore を包み、保存 Key・JSON 形式・更新と通知を一箇所にまとめる。画面・ViewModel・API 通信は持たない。

## 画面遷移
なし。

## 画面と状態
App で BookmarkRepository を一度生成し、search と bookmarks に同じインスタンスを渡す。読み込み済みのデータを保持し、書き込み成功後にだけ公開状態を更新する。呼び出し側は保存 Key や JSON 形式を扱わない。

## ユーザー操作と結果
- `load`、`contains`、`toggle`、`remove`、`saveNote` を提供する。
- 保存 Key は `github.bookmarks.v1`、JSON の version は 1。ID・表示用リポジトリ情報・メモ・保存日時の形式を維持し、移動前の保存データを読める。
- ブックマークは最大 100 件、メモは空欄可・最大 1,000 Unicode コードポイント。
- 更新は直列化し、同時操作で追加やメモを失わない。書き込み失敗時は公開状態を保つ。
- 読み込み失敗・壊れた JSON・未知または欠落した version はエラーとし、空データで上書きしない。

## 使う API とトークン
`core/storage`（KeyValueStore）と `core/network`（RepositorySummary）に依存する。生成 DTO は扱わない。外部 API やトークンは使わない。

## OS 固有の差分
iOS は @MainActor / Observation と Task の直列化、Android は StateFlow と Mutex を使用する。JSON の項目名と保存日時の単位（Unix 秒）は共通。

## シナリオ
- `specs/scenarios/github-bookmarks.yaml`

## エラーの表示
エラー型は種類と関連データを保持し、翻訳済みの表示文言を持たない。表示側が core/localization の文言に変換する。
