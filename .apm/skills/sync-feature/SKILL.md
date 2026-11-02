---
name: sync-feature
description: 機能や基盤モジュールの振る舞いを追加・変更するとき、spec.md を先に更新して iOS と Android を同じ構造で実装し、同期マーカーを更新するまでの手順。
---

# 機能の追加・変更（spec 先行で両 OS を揃える）

仕様の正は各モジュールの `spec.md`。実装より先に仕様を変える。

## 手順
1. 対象モジュール（`modules/features/<名前>` など）の `spec.md` を更新する。必要なら `shared/openapi`、`shared/tokens`、`shared/scenarios` も変える。
   - API を変えたら `shared/openapi/openapi.yaml` を先に直し、`python3 tools/generate.py` で `core/network` の DTO と APIClient を再生成する。ドメインモデルへの変換は `core/network` の手書きラッパーに足す。
   - 色・余白を変えたら `shared/tokens/tokens.json` を直し、`python3 tools/generate.py` で再生成する（`Generated/`・`generated/` は手で編集しない）。
2. 一方の OS を実装する（iOS: `<module>/ios/`、Android: `<module>/android/`）。
3. もう一方を**同じ型名・同じ構造**で実装する。意図した違いは `spec.md` の「OS 固有の差分」に書く。書かれていない違いはバグとして扱う。
4. 状態遷移やエラーを変えたら `shared/scenarios/*.yaml` を更新する。新しい step には `id` を付け、両 OS のテストに `// @scenario <シナリオ名>/<id>` を書いて同じ内容を実装する。
5. 追従を記録する: `python3 tools/check_spec_sync.py --update both <モジュールのパス>`（片方だけ追従したなら `ios` か `android`）。
6. 検査を通す: `python3 tools/check_spec_sync.py`、`python3 tools/check_deps.py`、`bash tools/check_generated.sh`、`python3 tools/check_scenarios.py`。
7. 両 OS の変更を 1 つの PR にまとめる。片方だけの PR にする場合は、もう片方の追従 Issue へのリンクを PR に書く。

## 注意
- `.spec-sync` は「その OS が追従済みの spec のハッシュ」。実装していないのに `--update` で記録しない。
- 依存ルールに反する依存を足さない（`features/*` 同士は禁止など）。詳しくは AGENTS.md。
