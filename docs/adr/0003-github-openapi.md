# ADR 0003 GitHub の公式 OpenAPI を必要な範囲に絞って使う

GitHub 閲覧サンプルでは、公式 github/rest-api-description の OpenAPI 3.0、API バージョン 2026-03-10 を使う。

公式定義のコミット SHA・パス・SHA-256 とライセンスを保存する。更新スクリプトが対象 operation の成功レスポンスとパラメータ、その参照 schema を抽出し、ローカルの原本スナップショットに保存する。アプリが使用するフィールドは明示的な選択定義で管理する。

通常の tools/generate.ts は原本と選択定義から shared/openapi/openapi.yaml を生成し、Swift / Kotlin を生成する。通常の生成・CI はネットワークに依存しない。公式定義の型・必須性・null 許容・enum を保持し、未対応構文は失敗させる。GitHub の全 API や SDK をアプリに持ち込まない。

生成 DTO とクライアントは core/network 内に閉じ、手書きの GitHubAPI ラッパーがドメインモデルとページ情報へ変換する。レスポンスヘッダーは各呼び出しの結果に保持し、Link とレート制限に利用する。

## 更新手順

固定元は `shared/openapi/github-source-meta.json`、フィールド選択は `github-selection.json`、取得した参照付き原本は `github-source.json` に記録する。通常はネットワークへ接続せず、保存済み入力だけで生成する。

```sh
# github/rest-api-description の更新対象コミットを指定
bun tools/sync-github-openapi.ts <40桁のコミットSHA>
bun tools/generate.ts
mise run check
```

取得元・APIバージョンは更新スクリプト内に固定している。APIバージョンを変更するときは、抽出元パスと生成契約・両OSのGitHubTransportヘッダーも合わせて変更する。更新差分で型・必須性・null許容とライセンスを確認し、生成物を入力と同じ変更に含める。
