# features/bookmarks

## 概要
「保存済み」タブとメモ編集シートを担当する。保存処理は `shared/bookmarks` の BookmarkRepository に委譲し、保存 Key や JSON 形式は扱わない。

## 画面遷移
| 画面（`specs/routes.yaml`） | 内容 |
| --- | --- |
| `bookmarks` | 「保存済み」タブ |
| `bookmarkNote` | `repositoryId` のメモを編集するシート |

一覧の選択は `push(repositoryDetail)`。AppRouter が search feature の入口へ対応付け、feature 同士は依存しない。メモは `present(bookmarkNote, sheet)`、保存成功・キャンセルは `dismiss()`。ID は文字列、保存データ内は 64bit 整数。

## 画面と状態
- 公開入口は BookmarksScreen と BookmarkNoteScreen。BookmarksDependencies と Navigator、メモ編集では repositoryId を受け取り、ViewModel の生成・保持と描画を feature 内に閉じる。
- 画面状態は NavigationEntry の ID ごとに保持し、タブ切り替えや戻る操作でも残った状態を維持する。
- 読み込み中・失敗・空一覧を表示する。一覧は保存時のリポジトリ情報を表示し、一覧のための API 通信は行わない。
- App で組み立てた同じ BookmarkRepository・DisplaySettings・ImageLoader を search と共有する。保存・解除・メモと説明文表示の変更を反映する。
- 行 UI に表示値と画像取得処理を渡し、画像取得失敗時は代替表示にする。

## ユーザー操作と結果
- 削除は確認ダイアログを表示する。成功後に一覧へ反映し、失敗時は保存済みデータを保ちエラーを表示する。
- メモは空欄可、最大 1,000 Unicode コードポイント。保存中は二重送信を防ぎ、失敗時は入力を残してシートを閉じない。キャンセルは下書きを保存せず閉じる。
- 文字数と長さの検証は `shared/bookmarks` の BookmarkNoteRules を使う。読み込み・保存中など画面状態による保存可否は ViewModel が判断する。
- 保存データの読み込み失敗・未知の保存バージョンはエラー表示し、空データで上書きしない。

## 使う API とトークン
外部 API は呼び出さない。`shared/bookmarks`、`core/navigation`、`core/network`（画像取得）、`core/storage`（表示設定）、`shared/designsystem` に依存する。他 feature には依存しない。

## OS 固有の差分
- モジュール名は iOS が `FeatureBookmarks`、Android のパッケージは `com.example.features.bookmarks`。 iOS の実装は `Sources/Features/Bookmarks`、テストは `Tests/Features/Bookmarks` に配置する。
- iOS は @MainActor / Observation と @State、Android は ViewModel / StateFlow とエントリごとの ViewModelStore。公開入口は Screen、内部描画は Swift の View / Kotlin の Content。
- iOS の List / シートと Android の LazyColumn / ボトムシートを使う。読み上げラベルと文字拡大に対応する。プロセス終了後の画面スタック復元は対象外。

## シナリオ
- `specs/scenarios/bookmark-note.yaml`

## ローカライズ
UI 文言は core/localization の Message を表示時の言語で解決する。ViewModel は翻訳前のキー・引数を保持する。通信・保存の失敗は意味に応じて表示し、未知のエラーの内部メッセージは表示しない。既存の状態遷移・再試行・キャンセル動作は維持する。API の本文・名前やユーザーのメモは翻訳しない。
