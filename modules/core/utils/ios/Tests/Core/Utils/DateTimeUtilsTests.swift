import CoreUtils
import Foundation
import Testing

struct DateTimeUtilsTests {
    @Test func usesExplicitLocaleAndTimeZone() throws {
        let date = Date(timeIntervalSince1970: 54_245)
        let utc = try #require(TimeZone(identifier: "UTC"))
        let tokyo = try #require(TimeZone(identifier: "Asia/Tokyo"))
        let british = Locale(identifier: "en_GB")
        #expect(DateTimeUtils.formatTime(date, locale: british, timeZone: utc) == "15:04:05")
        #expect(DateTimeUtils.formatTime(date, locale: british, timeZone: tokyo) == "00:04:05")
        #expect(DateTimeUtils.formatTime(date, locale: Locale(identifier: "ja_JP"), timeZone: utc) == "15:04:05")
        let american = DateTimeUtils.formatTime(date, locale: Locale(identifier: "en_US"), timeZone: utc)
        #expect(american.hasPrefix("3:04:05") && american.hasSuffix("PM"))
        #expect(DateTimeUtils.formatTime(date, locale: british, timeZone: utc) == "15:04:05")
    }

    @Test func observesDaylightSavingTransition() throws {
        let zone = try #require(TimeZone(identifier: "America/New_York"))
        let locale = Locale(identifier: "en_GB")
        let before = Date(timeIntervalSince1970: 1_710_053_999)
        let after = Date(timeIntervalSince1970: 1_710_054_000)
        #expect(DateTimeUtils.formatTime(before, locale: locale, timeZone: zone) == "01:59:59")
        #expect(DateTimeUtils.formatTime(after, locale: locale, timeZone: zone) == "03:00:00")
    }
}
