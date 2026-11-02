# core/network

## 概要
`shared/openapi` から生成した API クライアントを包み、ドメインモデルへの変換までを担う。画面層には生成された型を渡さない。

## 提供するもの
| 名前 | 内容 |
| --- | --- |
| `ItemsAPI.listItems()` | `shared/openapi` の `listItems` を呼び、`[Item]` を返す |
| `Item` | `id: String`, `title: String` |
| `DefaultItemsAPI(transport:)` | 生成された `APIClient` を使う `ItemsAPI` の実装 |
| `HTTPTransport` | 通信の差し替え口。`send(HTTPRequest) -> HTTPResponse` |
| `URLSessionTransport(baseURL:)` | 本番用の `HTTPTransport` |
| `APIError(status)` | 2xx 以外のレスポンスで投げるエラー |

## 内部（生成コード）
`python3 tools/generate.py` が `Generated/`（iOS）・`generated/`（Android）に生成する。モジュールの外には公開しない。

- schema ごとの DTO（`ItemDTO`）。JSON の未知のキーは無視し、省略可能な値は null を出力しない。
- operation ごとのメソッドを持つ `APIClient`。2xx 以外は `APIError` を投げる。パスに埋め込む値は 1 区切りとしてエンコードする（`/` もエスケープ）。

## OS 固有の差分
命名規約に合わせて、Android では略語を先頭だけ大文字にする: `ItemsApi` / `DefaultItemsApi` / `HttpTransport` / `HttpRequest` / `HttpResponse` / `ApiClient` / `ApiException`（iOS の `APIError`）/ `UrlConnectionTransport`（iOS の `URLSessionTransport`、ベース URL は `String`）。
JSON は iOS が `Codable`（`JSONDecoder`）、Android が kotlinx.serialization。リクエスト・レスポンスの body は iOS が `Data`、Android が `String`。
