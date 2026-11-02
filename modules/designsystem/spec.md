# designsystem

## 概要
`shared/tokens` から生成した値と、それを使う共通 UI 部品を置く。

## 提供するもの
| 名前 | 内容 |
| --- | --- |
| `Tokens` | 色・余白の定数（`tools/generate.py` で生成。手で編集しない） |

## OS 固有の差分
iOS 26 以降は Liquid Glass を使う部品を `#available` で分岐し、このモジュールの中に閉じ込める。
