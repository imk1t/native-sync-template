# core/navigation

## 概要
画面遷移の状態と操作を持つ（docs/adr/0002）。画面の一覧は `specs/routes.yaml` から生成する。描画はアプリ層が各 OS の部品で行い、このモジュールは UI に依存しない。

## 提供するもの
| 名前 | 内容 |
| --- | --- |
| `Route`（生成） | `specs/routes.yaml` の画面。引数を持つ画面は引数付き |
| `AppTab`（生成） | タブ。`root`（タブの先頭の画面）と `title` を持つ |
| `Route(path:)`（生成） | ディープリンクのパスから `Route` を作る。一致しなければ無し |
| `Route(url:)` | URL のエンコード済みパスを `Route(path:)` に渡す。パスを区切ってから各区切りを 1 回だけデコードする |
| `NavigationEntry` | 不変の `id: String`（既定は UUID）と `route: Route`。画面インスタンスを識別する |
| `ModalStyle` | `sheet` / `fullScreen` |
| `Modal` | `style` と `stack: [NavigationEntry]`（先頭がモーダルで開いた画面） |
| `NavigationState` | `selectedTab`、`roots`（タブごとの先頭エントリ）、`stacks`（全タブ分のエントリ。先頭は含めない）、`modals`（下から順） |
| `NavigationState.root(tab)` | タブの先頭エントリ。状態の更新やタブ切り替えで ID は変わらない |
| `NavigationState.opening(route, in: tab)` | ディープリンク用の状態。`route` がタブの先頭ならそのタブを空のスタックで選び、それ以外は `tab` のスタックに新しいエントリを 1 つ置く |
| `Navigator` | `state` を持ち、次の操作で書き換える |

## 操作
| 操作 | 結果 |
| --- | --- |
| `push(route)` | モーダルが無ければ選択中のタブのスタックに、あれば一番上のモーダルの `stack` に積む |
| `pop()` | モーダルがあれば一番上のモーダルの `stack` から 1 つ外す。`stack` が 1 つだけならそのモーダルを閉じる。モーダルが無ければ選択中のタブのスタックから 1 つ外す（空なら何もしない） |
| `popToRoot()` | モーダルがあれば一番上のモーダルの `stack` を先頭だけにする。無ければ選択中のタブのスタックを空にする |
| `present(route, style)` | `stack: [route]` のモーダルを一番上に重ねる。`style` の既定は `sheet` |
| `dismiss()` | 一番上のモーダルを閉じる（無ければ何もしない） |
| `selectTab(tab)` | すべてのモーダルを閉じて `tab` を選ぶ。各タブのスタックはそのまま |
| `reset(state)` | 状態を丸ごと差し替える |

`push` と `present` は呼び出しごとに新しいエントリ ID を発行する。同じ Route を複数回開いても別の画面として扱う。`pop`・`popToRoot`・タブ切り替えで残るエントリの ID は維持する。描画側も Route の値ではなくエントリ ID に画面状態と ViewModel を紐付ける。
ディープリンクのパスにはエンコード済みの文字列を渡す。たとえば `/items/a%2F1` の ID は `a/1`、`/items/a%252F1` の ID は `a%2F1` となる。

## OS 固有の差分
- 命名: Android は Kotlin の規約に合わせて先頭を大文字にする（`Route.ItemDetail(id)`、`AppTab.Home`、`ModalStyle.Sheet`）。`Route(path:)` は Android では `Route.fromPath(path)`、`opening(route, in:)` は `NavigationState.opening(route, tab)`。
- URL の入口は iOS が `Route(url: URL)`（URLComponents.percentEncodedPath）、Android が `Route.fromUrl(String)`（java.net.URI.rawPath）。Android アプリは Intent の URI 全体を文字列として渡す。
- 引数の `integer` は iOS が `Int`、Android が `Long`（どちらも 64 bit）。
- 状態の公開: iOS は `@Observable` の `state`（アプリ層が `Binding` で書き戻す。スワイプで戻る操作など）。Android は `StateFlow<NavigationState>` で、書き換えは操作経由のみ。
- 全タブを持たない `stacks` は、iOS は足りないタブを空のスタックで補い、Android は例外にする。
- 描画（アプリ層の AppRoot）: Android は各タブの decorated entries と ViewModelStore を常時保持し、表示する NavDisplay だけを切り替える。iOS の TabView も各タブの画面を保持する。各 feature の Screen がそのエントリ内で ViewModel を生成・保持し、タブを離れても取得済み状態を維持する。

## シナリオ
`specs/scenarios/navigation.yaml`
