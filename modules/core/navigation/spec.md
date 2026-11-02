# core/navigation

## 概要
画面遷移の状態と操作を持つ（docs/adr/0002）。画面の一覧は `shared/routes.yaml` から生成する。描画はアプリ層が各 OS の部品で行い、このモジュールは UI に依存しない。

## 提供するもの
| 名前 | 内容 |
| --- | --- |
| `Route`（生成） | `shared/routes.yaml` の画面。引数を持つ画面は引数付き |
| `AppTab`（生成） | タブ。`root`（タブの先頭の画面）と `title` を持つ |
| `Route(path:)`（生成） | ディープリンクのパスから `Route` を作る。一致しなければ無し |
| `ModalStyle` | `sheet` / `fullScreen` |
| `Modal` | `style` と `stack`（先頭がモーダルで開いた画面） |
| `NavigationState` | `selectedTab`、`stacks`（全タブ分。タブの先頭の画面は含めない）、`modals`（下から順） |
| `NavigationState.opening(route, in: tab)` | ディープリンク用の状態。`route` がタブの先頭ならそのタブを空のスタックで選び、それ以外は `tab` のスタックを `[route]` にする |
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

## OS 固有の差分
- 命名: Android は Kotlin の規約に合わせて先頭を大文字にする（`Route.ItemDetail(id)`、`AppTab.Home`、`ModalStyle.Sheet`）。`Route(path:)` は Android では `Route.fromPath(path)`、`opening(route, in:)` は `NavigationState.opening(route, tab)`。
- 引数の `integer` は iOS が `Int`、Android が `Long`（どちらも 64 bit）。
- 状態の公開: iOS は `@Observable` の `state`（アプリ層が `Binding` で書き戻す。スワイプで戻る操作など）。Android は `StateFlow<NavigationState>` で、書き換えは操作経由のみ。
- 全タブを持たない `stacks` は、iOS は足りないタブを空のスタックで補い、Android は例外にする。
- 描画（アプリ層）: Android はタブを切り替えると、選んでいないタブの画面の ViewModel を破棄する（スタックの内容は保持し、戻ったときに作り直す）。iOS の `TabView` は各タブの画面を保持する。

## シナリオ
`shared/scenarios/navigation.yaml`
