---
name: add-third-party
description: 外部ライブラリ（OSS・サードパーティ SDK）を iOS と Android に導入するときに使う。core のラッパーモジュール経由で使い、ADR・shared/third-party.yaml・バージョン管理を更新する手順。AndroidX・Kotlin・kotlinx など公式基盤ライブラリの追加には不要。
---

# 外部ライブラリの追加

## 原則
- 外部ライブラリは `core/*` のラッパーモジュール（例: `core/analytics`）の中だけで import する。feature からはラッパーの interface / protocol にだけ依存する。
- 公式機能で足りるものは入れない。

## 手順
1. 導入前に `docs/adr/` に ADR を書く: 目的、代替案、ライセンス、メンテナンス状況、もう一方の OS での対応物。
2. ラッパーモジュールを `add-module` の手順で作る。
3. バージョンの置き場所:
   - iOS: Swift Package Manager のみ（CocoaPods は使わない）。依存は `modules/Package.swift` に `.product(name:package:)` で書き、`Package.resolved` をコミットして全モジュールで 1 つのバージョンにする。
   - Android: `apps/android/gradle/libs.versions.toml` にだけ書く。BoM がある SDK は BoM で揃える。
4. `shared/third-party.yaml` に、役割・モジュール・iOS と Android それぞれのライブラリ名、ライセンス、担当を追記する。
5. 分析イベント名や設定キーなど両 OS で一致すべき名前は `shared/` に定義し、定数を生成する。
6. 環境ごとの設定ファイル（API キー、`GoogleService-Info.plist` や `google-services.json` など）はコミットしない。dev / prod をビルド設定（iOS の Configuration、Android の productFlavor）で切り替え、prod は CI のシークレットから注入する。
7. `bun tools/check-deps.ts` を通す。
