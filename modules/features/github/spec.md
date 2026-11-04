# features/github

## 概要
公開 GitHub リポジトリを検索・閲覧し、ブックマークとメモを端末内に保存する。Presentation / Domain / Data に分け、生成 DTO は扱わない。認証や GitHub への書き込みは行わない。

## 画面遷移
| 画面（`shared/routes.yaml`） | 内容 |
| --- | --- |
| `repositorySearch` | 「探す」タブ。検索語・言語・並べ替え、一覧 |
| `repositoryDetail` | `owner`, `repo` に対応する詳細 |
| `repositoryIssues` | `owner`, `repo` の Issue 一覧 |
| `issueDetail` | `owner`, `repo`, `number` の Issue 詳細 |
| `bookmarks` | 「保存済み」タブ |
| `bookmarkNote` | `repositoryId` のメモを編集するシート |

検索・保存済みの選択は `push(repositoryDetail)`、詳細から `push(repositoryIssues)`、Issue 選択は `push(issueDetail)`。メモは `present(bookmarkNote, sheet)`、保存成功・キャンセルは `dismiss()`。戻る操作は `pop()`。ID は文字列、API 内の ID は 64bit 整数。

## 画面と状態
- 各画面の公開入口は両 OS とも `<画面名>Screen` とし、画面引数・GitHubDependencies・必要な Navigator を受け取る。ViewModel の生成と保持、画面本体は feature 内に閉じる。アプリ層は AppRouter で Route から入口を呼ぶだけとする。
- 画面状態は NavigationEntry の ID ごとに保持する。同じ Route を複数回開いた場合は独立し、戻る操作やタブ切り替えでは残ったエントリの状態を維持する。
- 一覧は `items`, `hasLoaded`, `isLoading`, `error`, `nextPage`, `incomplete` を持つ。初期は未取得、空一覧。初回・更新・追加取得を区別し、失敗しても取得済みの一覧は保持する。
- 検索は確定時だけ取得。空白だけなら送信しない。言語は全て・Swift・Kotlin・TypeScript・Rust、並べ替えは関連度・Star 数・更新日時。検索語に `is:public` と選択言語を付ける。
- 1 ページ 20 件。ID で重複を除外し、終端は API の次ページ情報で判断する。検索の 1,000 件上限と不完全な結果を表示する。
- 詳細は取得中・取得成功・失敗を持つ。説明・言語・投稿者・本文が null でも表示できる。Issue 本文はプレーンテキスト。
- Issue は Open / Closed を選択する。PR は除外するが、PR だけのページでも次ページへの操作を残す。
- 画面スコープ内の初回取得は一度だけ。明示的な更新はキャッシュを迂回する。条件変更・キャンセル後の古い結果は反映しない。キャンセルはエラー表示せず、未取得なら再表示時に再試行できる。

## ユーザー操作と結果
- Repository は検索条件とページを含むキーで 5 分のメモリキャッシュを持つ。時計を注入できる。
- ブックマークは最大 100 件。ID、表示用リポジトリ情報、メモ、保存日時をバージョン付き JSON で保存する。一覧表示に追加 API 通信は不要。
- ブックマーク Repository を共有し、検索・詳細・保存済みへ変更を反映する。更新を直列化し、書き込み成功後に公開状態を更新する。
- メモは空欄可、最大 1,000 Unicode コードポイント。保存中は二重送信を防ぎ、失敗時は入力を残してシートを閉じない。削除は確認ダイアログを表示する。
- 保存データの読み込み失敗・未知の保存バージョンはエラーとし、空データで上書きしない。
- 設定の説明文表示を一覧に反映する。画像の取得は network の ImageLoader を使い、失敗時は代替表示にする。

## ユーザー操作と結果
検索確定で条件を更新し、一覧の選択で詳細を開く。保存ボタンはローカルの追加・解除、保存済みのメモ編集はシートを開く。保存成功後にだけ共有状態を更新し、失敗時には再試行できるエラーを表示する。

## 使う API とトークン
- API: `search/repos`, `repos/get`, `issues/list-for-repo`, `issues/get`。`core/network` の GitHubAPI を Repository で包む。
- `core/navigation`、`core/storage`、`designsystem` に依存。他 feature には依存しない。
- 共通 UI は designsystem の状態表示・バッジを利用する。

## OS 固有の差分
- iOS は @MainActor / Observation と画面ごとの @State、Android は ViewModel / StateFlow と Navigation 3 のエントリごとの ViewModelStore。iOS の task と Android の viewModelScope で取得を所有する。
- 公開入口は両 OS とも Screen。内部の描画は Swift の View / Kotlin の Content、API の略語は GitHubAPI / GitHubApi。Domain と Data の責務と型名は両 OS で揃える。
- iOS の List / Picker / シートと Android の LazyColumn / 選択チップ / ボトムシートを使う。読み上げラベルと文字拡大に対応する。プロセス終了後の画面スタック復元は対象外。

## シナリオ
- `shared/scenarios/github-search.yaml`
- `shared/scenarios/github-detail.yaml`
- `shared/scenarios/github-bookmarks.yaml`
