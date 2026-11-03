import CoreStorage
import Foundation
import Testing

@testable import FeatureSettings

@MainActor
struct SettingsViewModelTests {
    // @scenario settings/save-restore
    @Test func persistsSelection() async throws {
        let store = InMemoryKeyValueStore()
        let model = SettingsViewModel(settings: DisplaySettings(store: store))
        await model.load()
        model.showDescriptions = false
        await model.save()
        let restored = DisplaySettings(store: store)
        try await restored.load()
        #expect(!restored.showDescriptions)
    }
    // @scenario settings/write-fails
    @Test func failedWriteKeepsSharedSetting() async {
        let settings = DisplaySettings(store: UnwritableStore())
        let model = SettingsViewModel(settings: settings)
        await model.load()
        model.showDescriptions = false
        await model.save()
        #expect(settings.showDescriptions)
        #expect(model.showDescriptions && model.error != nil)
    }
}

private struct UnwritableStore: KeyValueStore {
    func get(_ key: String) async throws -> String? { nil }
    func set(_ key: String, _ value: String?) async throws { throw CocoaError(.fileWriteNoPermission) }
}
