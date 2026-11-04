---
name: sync-feature
description: 機能や基盤モジュールの振る舞い（画面、画面遷移、状態、API、デザイントークン、シナリオ）を追加・変更し、iOS と Android を同じ構造で揃えるときに使う。spec.md を先に更新し、同期マーカーを記録するまでの手順。新しいモジュールの作成は add-module、外部ライブラリは add-third-party を使う。
---

# 機能の追加・変更（spec 先行で両 OS を揃える）

仕様の正は各モジュールの `spec.md`。実装より先に仕様を変える。

## 手順
1. 対象モジュールの `spec.md` を更新する。必要なら `specs/openapi`、`specs/tokens`、`specs/scenarios` も変える。
   - API: `specs/openapi/openapi.yaml` を直し、`bun tools/generate.ts` で `core/network` の DTO と APIClient を再生成する。ドメインモデルへの変換は `core/network` の手書きラッパーに足す。
   - 色・余白: `specs/tokens/tokens.json` を直し、`bun tools/generate.ts` で再生成する。
   - 画面: `specs/routes.yaml` に足して再生成し、spec.md の「画面遷移」に画面と遷移（`push(...)` など）を書く。遷移は ViewModel から `Navigator` を呼ぶ。各 feature の公開入口 `*Screen` で ViewModel を生成・保持し、両 OS のアプリ層の `AppRouter` に `Route` → 入口の対応を足す。アプリ層には個別画面やその ViewModel の生成処理を置かない。
2. 一方の OS を実装する（iOS: `<module>/ios/`、Android: `<module>/android/`）。
3. もう一方を**同じ型名・同じ構造**で実装する。意図した違いは `spec.md` の「OS 固有の差分」に書く。
4. 状態遷移やエラーを変えたら `specs/scenarios/*.yaml` を更新する。新しい step には `id` を付け、両 OS のテストに `// @scenario <シナリオ名>/<id>` を書く。
5. `bun tools/check-spec-sync.ts --update both <モジュールのパス>` で追従を記録する（片方だけなら `ios` か `android`）。
6. AGENTS.md の「コマンド」の検査をすべて通す。
7. 両 OS の変更を 1 つの PR にまとめる。片方だけの PR にする場合は、もう片方の追従 Issue へのリンクを書く。

## 注意
- `.spec-sync` は「その OS が追従済みの spec のハッシュ」。実装していないのに `--update` で記録しない。
- 依存ルールは AGENTS.md に従う。
