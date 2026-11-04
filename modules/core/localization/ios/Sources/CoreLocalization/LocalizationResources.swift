import Foundation

public enum LocalizationResources {
    public static let bundle: Bundle = .module

    /// A language outside the published set uses the package's development language.
    static func supportedLocale(_ locale: Locale) -> Locale {
        let supported = bundle.localizations.contains {
            Locale(identifier: $0).language.languageCode == locale.language.languageCode
        }
        if supported { return locale }
        return Locale(identifier: bundle.developmentLocalization ?? locale.identifier)
    }
}
