# core/storage

## 概要
端末内に小さな値を保存する。設定などの普通の値は `KeyValueStore`、認証トークンなどの秘密情報は `SecureStore` に分ける。どちらも OS 標準・AndroidX だけで実装し、外部 SDK は使わない。
一覧データのキャッシュのような構造化データ（SwiftData / Room）は、必要な機能が出てきたときに別途追加する。

## 提供するもの
| 名前 | 内容 |
| --- | --- |
| `KeyValueStore` | `get(key) -> String?` / `set(key, value)`。`value` に null を渡すと削除する。非同期で、失敗すると例外を投げる |
| `SecureStore` | `KeyValueStore` と同じ操作。秘密情報専用で、`KeyValueStore` とは型で分ける |
| `UserDefaultsStore(suiteName:)` / `DataStoreKeyValueStore(dataStore)` | `KeyValueStore` の実装 |
| `KeychainSecureStore(service:)` / `KeystoreSecureStore(dataStore, cipher)` | `SecureStore` の実装 |
| `InMemoryKeyValueStore` / `InMemorySecureStore` | テストやプレビュー用の実装 |

値は文字列だけを扱う。数値や構造体は呼び出し側で文字列（JSON など）にする。両 OS で保存できる型を揃えるため。
`SecureStore.set` は既存値を先に削除しない。更新が失敗した場合は例外を返し、更新前の値を保持する。削除するのは value が null の場合だけ。

## OS 固有の差分
- iOS の `KeyValueStore` は UserDefaults、Android は Preferences DataStore。DataStore は 1 ファイルにつき 1 インスタンスにする必要があるため、`DataStore<Preferences>` はアプリで 1 回だけ作って渡す（`DataStoreKeyValueStore.create(context, name)`）。
- iOS の `SecureStore` は Keychain（`kSecClassGenericPassword`、`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`）。Android には Keychain に当たる値の保管庫が無いので、Android Keystore の AES-GCM 鍵で暗号化した値を DataStore に保存する。暗号化は `ValueCipher`（実装は `AndroidKeystoreCipher(alias)`）に分け、JVM のテストでは差し替える。
- iOS の更新は SecItemUpdate を使い、未登録のキーだけ SecItemAdd で追加する。Android は暗号化の成功後に DataStore のトランザクションで更新する。
- 同じ理由で iOS は SecItem の呼び出しを内部の `KeychainItems` に分け、テストでは差し替える。ホストアプリの無いパッケージのテストでは Keychain の entitlement が無く、本物の Keychain を使えない（`errSecMissingEntitlement`）ため。本物の Keychain の確認は、アプリをホストにしたテストで行う。
