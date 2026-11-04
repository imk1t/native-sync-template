import Foundation
import Testing
@testable import CoreLocalization

struct MessageTests {
    // @scenario localization/language-change
    @Test func nativeCatalogAndLocaleChanges() {
        let message = Message.commonRetry
        #expect(message.resolve(locale: Locale(identifier: "ja")) == "再試行")
        #expect(message.resolve(locale: Locale(identifier: "en")) == "Retry")
        #expect(message.resolve(locale: Locale(identifier: "fr")) == "再試行")
    }

    // @scenario localization/plural-arguments
    @Test func pluralAndArguments() {
        let english = Locale(identifier: "en")
        #expect(Message.repositoryStars(count: 1).resolve(locale: english) == "1 star")
        #expect(Message.repositoryStars(count: 2).resolve(locale: english) == "2 stars")
        #expect(Message.repositoryStars(count: 0).resolve(locale: english) == "0 stars")
        #expect(Message.repositoryStars(count: 2).resolve(locale: Locale(identifier: "ja")) == "Star 2")
        #expect(Message.searchFilter(label: "Language", value: "Swift").resolve(locale: english) == "Language: Swift")
        #expect(Message.errorServer(status: 503).resolve(locale: english).contains("503"))
    }
}
