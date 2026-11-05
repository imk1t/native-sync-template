# core/storage

## Overview

Persist small values on the device. Use `KeyValueStore` for ordinary values such as settings and `SecureStore` for secrets such as authentication tokens. Both use platform APIs and AndroidX only, without third-party SDKs.

Add structured storage such as SwiftData / Room separately when a feature requires it, for example to cache lists.

## Public API

| Name | Behavior |
| --- | --- |
| `KeyValueStore` | `get(key) -> String?` / `set(key, value)`; passing null deletes the value. Asynchronous, throwing on failure |
| `SecureStore` | The same operations as KeyValueStore, with a separate type for secrets |
| `UserDefaultsStore(suiteName:)` / `DataStoreKeyValueStore(dataStore)` | KeyValueStore implementations |
| `KeychainSecureStore(service:)` / `KeystoreSecureStore(dataStore, cipher)` | SecureStore implementations |
| `InMemoryKeyValueStore` / `InMemorySecureStore` | Test and preview implementations |

Values are strings only. Callers encode numbers and structures as strings, such as JSON, to keep supported storage types aligned across platforms.

`SecureStore.set` must not delete the existing value before updating. If an update fails, throw and preserve the previous value. Delete only when value is null.

## Display settings

DisplaySettings persists showDescriptions, initially true, through KeyValueStore. Share the initial load and serialize writes. Update public state only after a successful write. Propagate read/write failures to callers. Use @MainActor / Observation on iOS and StateFlow on Android to update all screens.

## Platform differences

- The iOS module is `CoreStorage`, with implementation in `Sources/Core/Storage` and tests in `Tests/Core/Storage`. The Android package is `com.example.core.storage`.
- KeyValueStore uses UserDefaults on iOS and Preferences DataStore on Android. DataStore requires one instance per file: call `DataStoreKeyValueStore.persistent(context, name)` once in the Application dependency graph and share it. Use `create(context, name)` when injecting DataStore directly.
- SecureStore uses Keychain on iOS (`kSecClassGenericPassword`, `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`). Android has no equivalent value vault, so encrypt values with an Android Keystore AES-GCM key and store them in DataStore. Isolate encryption behind ValueCipher, implemented by AndroidKeystoreCipher(alias), so JVM tests can replace it.
- iOS updates use SecItemUpdate, falling back to SecItemAdd only for missing keys. Android updates through a DataStore transaction after encryption succeeds.
- Likewise, isolate iOS SecItem calls behind internal KeychainItems for tests. Package tests without a host app lack Keychain entitlements and cannot use the real Keychain (`errSecMissingEntitlement`). Verify the real Keychain with app-hosted tests.
