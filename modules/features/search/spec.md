# features/search

## 概要
「探す」タブとリポジトリ・Issue の閲覧を担当する。Presentation / Domain / Data に分け、生成 DTO は扱わない。認証や GitHub への書き込みは行わない。

## 画面遷移
| 画面（`specs/routes.yaml`） | 内容 |
| --- | --- |
| `repositorySearch` | 「探す」タブ。検索語・言語・並べ替え、一覧 |
| `repositoryDetail` | `owner`, `repo` に対応する詳細。保存済みからも同じ入口を開く |
| `repositoryIssues` | `owner`, `repo` の Issue 一覧 |
| `issueDetail` | `owner`, `repo`, `number` の Issue 詳細 |

検索の選択は `push(repositoryDetail)`、詳細から `push(repositoryIssues)`、Issue 選択は `push(issueDetail)`。戻る操作は `pop()`。API 内の ID は 64bit 整数。

## 画面と状態
- 公開入口は両 OS とも `<画面名>Screen` とし、画面引数・SearchDependencies・必要な Navigator を受け取る。ViewModel の生成と保持、画面本体は feature 内に閉じる。アプリ層は AppRouter で Route から入口を呼ぶだけとする。
- 画面状態は NavigationEntry の ID ごとに保持する。同じ Route を複数回開いた場合は独立し、戻る操作やタブ切り替えでは残ったエントリの状態を維持する。
- 一覧は `items`, `hasLoaded`, `isLoading`, `error`, `nextPage`, `incomplete` を持つ。初期は未取得、空一覧。初回・更新・追加取得を区別し、失敗しても取得済みの一覧は保持する。
- 検索は確定時だけ取得。空白だけなら送信しない。言語は全て・Swift・Kotlin・TypeScript・Rust、並べ替えは関連度・Star 数・更新日時。検索語に `is:public` と選択言語を付ける。
- 1 ページ 20 件。ID で重複を除外し、終端は API の次ページ情報で判断する。検索の 1,000 件上限と不完全な結果を表示する。
- 詳細は取得中・取得成功・失敗を持つ。説明・言語・投稿者・本文が null でも表示できる。Issue 本文はプレーンテキスト。
- Issue は Open / Closed を選択する。PR は除外するが、PR だけのページでも次ページへの操作を残す。
- 画面スコープ内の初回取得は一度だけ。明示的な更新はキャッシュを迂回する。条件変更・キャンセル後の古い結果は反映しない。キャンセルはエラー表示せず、未取得なら再表示時に再試行できる。

## ユーザー操作と結果
- GitHubRepository は検索条件とページを含むキーで 5 分のメモリキャッシュを持つ。時計を注入できる。
- 保存・解除は `shared/bookmarks` の BookmarkRepository に委譲する。同じインスタンスを保存済み feature と共有し、書き込み成功後の変更を一覧と詳細に反映する。失敗時は再試行できるエラーを表示する。
- 保存データの読み込み失敗は表示し、空データで上書きしない。
- 共有 DisplaySettings の説明文表示を一覧に反映する。行 UI に表示値と ImageLoader の取得処理を渡し、画像取得失敗時は代替表示にする。

## 使う API とトークン
- API: `search/repos`, `repos/get`, `issues/list-for-repo`, `issues/get`。`core/network` の GitHubAPI を Repository で包む。
- `core/navigation`、`core/network`、`core/storage`、`shared/bookmarks`、`shared/designsystem` に依存。他 feature には依存しない。
- 共通 UI は `shared/designsystem` の行・状態表示・バッジを利用する。

## OS 固有の差分
- モジュール名は iOS が `FeatureSearch`、Android のパッケージは `com.example.features.search`。
- iOS は @MainActor / Observation と画面ごとの @State、Android は ViewModel / StateFlow と Navigation 3 のエントリごとの ViewModelStore。iOS の task と Android の viewModelScope で取得を所有する。
- 公開入口は両 OS とも Screen。内部の描画は Swift の View / Kotlin の Content、API の略語は GitHubAPI / GitHubApi。Domain と Data の責務と型名は両 OS で揃える。
- iOS の List / Picker と Android の LazyColumn / 選択チップを使う。読み上げラベルと文字拡大に対応する。プロセス終了後の画面スタック復元は対象外。

## シナリオ
- `specs/scenarios/github-search.yaml`
- `specs/scenarios/github-detail.yaml`

## ローカライズ
UI 文言は core/localization の Message を表示時の言語で解決する。ViewModel は翻訳前のキー・引数を保持する。通信・保存の失敗は意味に応じて表示し、未知のエラーの内部メッセージは表示しない。既存の状態遷移・再試行・キャンセル動作は維持する。API の本文・名前やユーザーのメモは翻訳しない。
