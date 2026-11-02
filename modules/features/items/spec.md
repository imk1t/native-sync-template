# features/items

## 概要
アイテム一覧を API から取得して表示し、選んだアイテムの詳細を表示する。サンプルの機能。

## 画面遷移
| 画面（`shared/routes.yaml`） | 内容 |
| --- | --- |
| `items` | 一覧。ホームタブの先頭 |
| `itemDetail` | 詳細。引数 `id` |

| 操作 | 遷移 |
| --- | --- |
| 一覧でアイテムを選ぶ | `push(itemDetail(id))` |

## 画面と状態
一覧（`ItemsViewModel`）と詳細（`ItemDetailViewModel`）は同じ状態の形を持つ。

| 状態 | 一覧 | 詳細 |
| --- | --- | --- |
| `loading` | 読み込み中 | 読み込み中 |
| `loaded` | `loaded(items)` 一覧を表示 | `loaded(item)` タイトルと ID を表示 |
| `error` | 読み込みに失敗 | 読み込みに失敗 |

初期状態は `loading`。`load` で `loaded` か `error` に遷移する。

## ユーザー操作と結果
| 操作 | 結果 |
| --- | --- |
| 画面を開く / 再読み込み | `load` を実行する |
| 一覧でアイテムを選ぶ（`select(item)`） | 詳細へ遷移する |

画面の初回表示では `loadIfNeeded` で一度だけ読み込む。画面を所有するスコープが続いている間は、ビューの再評価や Android の画面回転で ViewModel と取得済みの状態を作り直さず、API を再取得しない。明示的な再読み込みには `load` を使う。
画面の所有スコープが終了したら、そのスコープの読み込み処理をキャンセルする。キャンセルは通信失敗として表示せず、初回読み込みがキャンセルされた場合は次の表示で再試行できる。

## 使う API とトークン
- API: `listItems`（一覧）、`getItem`（詳細）
- トークン: `color.primary`, `spacing.medium`
- 画面遷移: `core/navigation` の `Navigator`

## OS 固有の差分
画面の所有スコープは、Navigator のスタック上の 1 画面（エントリ）。iOS はアプリ層が画面ごとに `@State` で ViewModel を所有し、`.task` のキャンセルに従う。Android は Navigation 3 のエントリごとの `ViewModelStore` で所有し、構成変更では保持、スタックから外れると `viewModelScope` をキャンセルする。プロセス終了後の状態復元は対象外。
型名は Android では `Route.ItemDetail(id)` のように先頭を大文字にする（`core/navigation` の差分に従う）。

## シナリオ
- `shared/scenarios/items-list.yaml`
- `shared/scenarios/item-detail.yaml`
