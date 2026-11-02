# features/items

## 概要
アイテム一覧を API から取得して表示する。サンプルの機能。

## 画面と状態
| 状態 | 内容 |
| --- | --- |
| `loading` | 読み込み中 |
| `loaded(items)` | 一覧を表示 |
| `error` | 読み込みに失敗 |

初期状態は `loading`。`load` で `loaded` か `error` に遷移する。

## ユーザー操作と結果
| 操作 | 結果 |
| --- | --- |
| 画面を開く / 再読み込み | `load` を実行する |

画面の初回表示では `loadIfNeeded` で一度だけ読み込む。画面を所有するスコープが続いている間は、ビューの再評価や Android の画面回転で ViewModel と取得済みの状態を作り直さず、API を再取得しない。明示的な再読み込みには `load` を使う。
画面の所有スコープが終了したら、そのスコープの読み込み処理をキャンセルする。キャンセルは通信失敗として表示せず、初回読み込みがキャンセルされた場合は次の表示で再試行できる。

## 使う API とトークン
- API: `listItems`
- トークン: `color.primary`, `spacing.medium`

## OS 固有の差分
iOS は画面ルートの `@State` で ViewModel を所有し、`.task` のキャンセルに従う。Android は Activity の `ViewModelStore` で所有し、構成変更では保持、Activity の終了では `viewModelScope` をキャンセルする。プロセス終了後の状態復元は対象外。

## シナリオ
`shared/scenarios/items-list.yaml`
