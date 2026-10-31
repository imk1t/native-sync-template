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

## 使う API とトークン
- API: `listItems`
- トークン: `color.primary`, `spacing.medium`

## OS 固有の差分
なし

## シナリオ
`shared/scenarios/items-list.yaml`
