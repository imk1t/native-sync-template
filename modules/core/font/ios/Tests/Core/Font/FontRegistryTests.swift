import Testing

@testable import CoreFont

struct FontRegistryTests {
    @Test func registersBundledFaces() {
        FontRegistry.registerAll()
        FontRegistry.registerAll()  // 2 回目も失敗しない
        for name in ["InterTight-Regular", "InterTight-Medium", "InterTight-Bold", "JetBrainsMono-Medium"] {
            #expect(FontRegistry.isAvailable(postScriptName: name), "\(name) を登録できていない")
        }
    }

    @Test func looksUpBundledFace() {
        #expect(FontRegistry.postScriptName(family: "Inter Tight", weight: 400) == "InterTight-Regular")
        #expect(FontRegistry.postScriptName(family: "Inter Tight", weight: 700) == "InterTight-Bold")
        #expect(FontRegistry.postScriptName(family: "JetBrains Mono", weight: 500) == "JetBrainsMono-Medium")
    }

    @Test func returnsNilForUnbundledFaces() {
        #expect(FontRegistry.postScriptName(family: "Inter Tight", weight: 600) == nil)
        #expect(FontRegistry.postScriptName(family: "Noto Sans JP", weight: 400) == nil)
        #expect(!FontRegistry.isAvailable(postScriptName: "NoSuchFont-Regular"))
    }
}
