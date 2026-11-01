---
name: add-third-party
description: Firebase など外部ライブラリ（OSS・SDK）を iOS と Android に追加するとき、ラッパーモジュール経由で使い、対応表とバージョンを更新する手順。
---

# 外部ライブラリの追加

## 原則
- 外部ライブラリは `core/*` のラッパーモジュール（例: `core/analytics`、`core/crashreporting`、`core/remoteconfig`）の中だけで import する。feature からはラッパーの interface / protocol にだけ依存する。
- 公式機能で足りるものは入れない。

## 手順
1. 導入を決める前に `docs/adr/` に ADR を書く: 目的、代替案、ライセンス、メンテナンス状況、もう一方の OS での対応物。
2. ラッパーモジュールを `add-module` の手順で作る（`core/utils` 以外の外部依存を持てるのは `core/*` のみ）。
3. バージョンを置く場所:
   - iOS: Swift Package Manager のみ（CocoaPods は使わない）。依存は `modules/Package.swift` に書き、`Package.resolved` をコミットして全モジュールで 1 つのバージョンにする。
   - Android: `apps/android/gradle/libs.versions.toml` にだけ書く。Firebase は BoM でバージョンを揃える。
4. `shared/third-party.yaml` に、役割・モジュール・iOS と Android それぞれのライブラリ名とライセンス・担当を追記する。
5. 分析イベント名や Remote Config のキーは `shared/` に定義して両 OS の定数を生成する（片方だけ名前が違う事故を防ぐ）。
6. Firebase の設定ファイル（`GoogleService-Info.plist`、`google-services.json`）はコミットしない。dev / prod ごとに用意して、ビルド設定（iOS の Configuration、Android の productFlavor）で切り替え、prod は CI のシークレットから注入する。
7. `python3 tools/check_deps.py` を通す。
