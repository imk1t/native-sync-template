# core/network

## 概要
生成された API クライアントを包み、ドメインモデルへの変換までを担う。画面層には生成された型を渡さない。

## 提供するもの
| 名前 | 内容 |
| --- | --- |
| `ItemsAPI.listItems()` | `shared/openapi` の `listItems` を呼び、`[Item]` を返す |
| `Item` | `id: String`, `title: String` |

## OS 固有の差分
なし
