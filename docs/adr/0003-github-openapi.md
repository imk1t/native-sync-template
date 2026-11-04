# ADR 0003 GitHub の API 契約を OpenAPI で直接管理する

GitHub 閲覧サンプルでは、公式 OpenAPI を元にアプリが使用する4操作と必要なフィールドを定義した `shared/openapi/openapi.yaml` を API 契約の正として直接管理する。OpenAPI 3.0、GitHub API バージョン 2026-03-10 を使う。

元になった公式定義は `github/rest-api-description` のコミット `6348fd455a44a445f9e37a505e981b2c3ed24046` にある `descriptions/api.github.com/api.github.com.2026-03-10.json`。これは初期契約の出典であり、現在の契約を自動的に同期するものではない。ライセンスは `shared/openapi/GITHUB-LICENSE.md` に保持する。

`tools/generate.ts` は `openapi.yaml` を読んで Swift / Kotlin の DTO とクライアントを生成し、契約自体は書き換えない。生成・CI はネットワークに依存しない。CI は再生成による差分と共通シナリオの契約適合を検査し、対応範囲外の構文は生成時にエラーとする。

生成 DTO とクライアントは core/network 内に閉じ、手書きの GitHubAPI ラッパーがドメインモデルとページ情報へ変換する。レスポンスヘッダーは各呼び出しの結果に保持し、Link とレート制限に利用する。

## 更新手順

公式の API 仕様を確認して `shared/openapi/openapi.yaml` を直接編集する。型・必須性・null 許容・enum を確認し、ID は両 OS で 64bit とするため `format: int64` を指定する。必要に応じて spec・共通シナリオ・テストデータ・手書きラッパーも更新する。

```sh
bun tools/generate.ts
mise run check
```

生成物は契約と同じ変更に含め、Swift / Kotlin を変更した場合は `mise run lint` と対象のネイティブテストも実行する。GitHub API バージョンを変更するときは、契約の `info.version` と両 OS の GitHubTransport のヘッダーも合わせて変更する。

テンプレートの保守を簡潔にするため、公式定義を取得・抽出する専用処理と選択設定は持たない。そのため公式定義との一致は自動検査せず、API 変更時の確認とレビューで維持する。
