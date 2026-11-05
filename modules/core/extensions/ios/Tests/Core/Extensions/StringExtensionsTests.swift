import CoreExtensions
import Testing

struct StringExtensionsTests {
    @Test func whitespaceContract() {
        let whitespace = "\u{0009}\u{000A}\u{000B}\u{000C}\u{000D}\u{0020}\u{0085}\u{00A0}\u{1680}"
            + "\u{2000}\u{2001}\u{2002}\u{2003}\u{2004}\u{2005}\u{2006}\u{2007}\u{2008}\u{2009}\u{200A}"
            + "\u{2028}\u{2029}\u{202F}\u{205F}\u{3000}"
        #expect("".isBlankOrEmpty)
        #expect("".trimmedWhitespace() == "")
        #expect(whitespace.isBlankOrEmpty)
        #expect(whitespace.trimmedWhitespace() == "")
        #expect((whitespace + "swift  kotlin\n日本語" + whitespace).trimmedWhitespace() == "swift  kotlin\n日本語")
        for text in ["\u{001C}", "\u{001D}", "\u{001E}", "\u{001F}", "\u{200B}", "\u{FEFF}"] {
            #expect(!text.isBlankOrEmpty)
            #expect((whitespace + text + whitespace).trimmedWhitespace() == text)
        }
        #expect(" \u{0301}x ".trimmedWhitespace().unicodeScalars.first?.value == 0x0301)
    }

    @Test func countsCodePointsWithoutNormalizing() {
        for (text, count) in [("", 0), ("abc", 3), ("日本語", 3), ("😀", 1), ("é", 1),
                              ("e\u{0301}", 2), ("👨‍👩‍👧‍👦", 7), ("🇯🇵", 2)] {
            #expect(text.codePointCount == count)
            #expect((" " + text + " ").trimmedWhitespace().codePointCount == count)
        }
    }
}
