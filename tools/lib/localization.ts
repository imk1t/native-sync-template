/** 言語別 YAML を検証し、OS 標準リソースと型付き参照を生成する。 */
import { parseDocument } from "yaml";
import { readdirSync, rmSync } from "node:fs";
import { isDict, join, readText, type Dict } from "./util";

export const DEFAULT_LOCALE = "ja";
export const IOS = "modules/core/localization/ios/Sources/Core/Localization/Generated";
export const ANDROID = "modules/core/localization/android/src/main/generated";
export const ROOTS = [
  IOS,
  ANDROID,
  "modules/Generated/Localization",
  "apps/ios/App/Generated/Localization",
  "apps/ios/Generated/Localization",
  "apps/android/app/src/main/generated/localization",
];
type Argument = { name: string; type: "string" | "integer" };
type Entry = {
  key: string;
  arguments: Argument[];
  iosOrder: string[];
  plural?: string;
  comment?: string;
  iosInfoPlist?: string;
};
type Translation = { text?: string; forms?: Record<string, string> };
export type Model = { locales: string[]; entries: Entry[]; translations: Record<string, Record<string, Translation>> };
const IDENT = /^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$/;
const ARGUMENT = /^[a-z][a-zA-Z0-9]*$/;
const RESERVED = new Set(
  `
  as associatedtype break case catch class continue default defer deinit do else enum extension fallthrough false
  fileprivate for func guard if import in init inout internal is let nil open operator private protocol public repeat
  rethrows return self static struct subscript super switch throw throws true try typealias var where while
  abstract actual annotation by companion const constructor crossinline data delegate dynamic expect external final
  finally fun get infix inline inner interface lateinit noinline null object out override param property protected
  receiver reified sealed set setparam suspend tailrec this typealias typeof val vararg when context locale resolve
`
    .trim()
    .split(/\s+/),
);
const CATEGORIES = ["zero", "one", "two", "few", "many", "other"];
const fail = (message: string): never => {
  throw new Error(`localization: ${message}`);
};
const camel = (s: string) => s.replace(/_([a-z0-9])/g, (_, c: string) => c.toUpperCase());
const cap = (s: string) => s[0]!.toUpperCase() + s.slice(1);
const quote = (s: string) => JSON.stringify(s);

function keys(value: Dict, allowed: string[], where: string): void {
  for (const key of Object.keys(value)) if (!allowed.includes(key)) fail(`${where}: 未対応のフィールド ${key}`);
}
function nonempty(value: unknown, where: string): string {
  if (typeof value !== "string" || !value.trim() || /[\u0000-\u0008\u000b\u000c\u000e-\u001f]/.test(value))
    fail(`${where}: 空でない文字列が必要です`);
  return value as string;
}
function entry(value: unknown, key: string, base: boolean): Dict {
  if (typeof value === "string") return { text: value };
  if (!isDict(value)) return fail(`${key}: 文字列または辞書が必要です`);
  keys(value, base ? ["text", "forms", "arguments", "plural", "comment", "iosInfoPlist"] : ["text", "forms"], key);
  return value;
}
export function load(root: string): Model {
  const dir = join(root, "specs/localization");
  const files = readdirSync(dir)
    .filter((f) => f.startsWith("messages-") || f.endsWith(".yaml"))
    .sort();
  const docs: Record<string, Dict> = {};
  for (const file of files) {
    const matchedLocale = /^messages-([a-z]{2,3}(?:-[A-Z][a-z]{3})?(?:-[A-Z]{2}|-\d{3})?)\.yaml$/.exec(file)?.[1];
    const locale = matchedLocale ?? fail(`非対応のファイル名 ${file}（BCP 47 の language[-Script][-REGION]）`);
    if (Intl.getCanonicalLocales(locale)[0] !== locale || Intl.DateTimeFormat.supportedLocalesOf([locale]).length === 0)
      fail(`未対応言語 ${locale}`);
    const document = parseDocument(readText(join(dir, file)), { uniqueKeys: true });
    if (document.errors.length || document.warnings.length)
      fail(`${file}: ${[...document.errors, ...document.warnings].map((e) => e.message).join("; ")}`);
    const doc: unknown = document.toJS({ maxAliasCount: 0 });
    if (!isDict(doc) || !Object.keys(doc).length) fail(`${file}: メッセージの辞書が必要です`);
    docs[locale] = doc as Dict;
  }
  const base = docs[DEFAULT_LOCALE] ?? fail(`messages-${DEFAULT_LOCALE}.yaml が必要です`);
  const entries: Entry[] = [];
  const identifiers = new Set<string>();
  for (const key of Object.keys(base).sort()) {
    if (!IDENT.test(key)) fail(`キーは snake_case: ${key}`);
    if (RESERVED.has(camel(key)) || identifiers.has(camel(key))) fail(`${key}: 生成される識別子が予約語または重複です`);
    identifiers.add(camel(key));
    const raw = entry(base[key], key, true);
    const args: Argument[] = [];
    if (raw.arguments !== undefined) {
      if (!Array.isArray(raw.arguments) || raw.arguments.length === 0) fail(`${key}: arguments は空でない配列`);
      for (const arg of raw.arguments as unknown[]) {
        if (!isDict(arg)) fail(`${key}: 不正な引数`);
        const a = arg as Dict;
        keys(a, ["name", "type"], key);
        if (typeof a.name !== "string" || !ARGUMENT.test(a.name) || RESERVED.has(a.name)) fail(`${key}: 不正な引数名`);
        if (args.some((x) => x.name === a.name)) fail(`${key}: 引数名が重複`);
        if (a.type !== "string" && a.type !== "integer") fail(`${key}: 引数は string / integer`);
        args.push({ name: a.name as string, type: a.type as Argument["type"] });
      }
    }
    const reference =
      typeof raw.text === "string"
        ? raw.text
        : isDict(raw.forms) && typeof raw.forms.other === "string"
          ? raw.forms.other
          : "";
    const iosOrder = [...reference.matchAll(/\{([a-zA-Z][a-zA-Z0-9]*)\}/g)].map((m) => m[1]!);
    const e: Entry = { key, arguments: args, iosOrder };
    if (raw.plural !== undefined) {
      if (!args.some((a) => a.name === raw.plural && a.type === "integer")) fail(`${key}: plural は整数引数を指定`);
      e.plural = raw.plural as string;
    }
    if (raw.comment !== undefined) e.comment = nonempty(raw.comment, key);
    if (raw.iosInfoPlist !== undefined) {
      if (raw.iosInfoPlist !== "CFBundleDisplayName" || args.length > 0) fail(`${key}: 未対応の InfoPlist 定義`);
      if (entries.some((x) => x.iosInfoPlist === raw.iosInfoPlist)) fail(`${key}: InfoPlist キーの重複`);
      e.iosInfoPlist = raw.iosInfoPlist as string;
    }
    entries.push(e);
  }
  const translations: Model["translations"] = {};
  for (const [locale, doc] of Object.entries(docs)) {
    if (Object.keys(doc).sort().join("\0") !== entries.map((e) => e.key).join("\0"))
      fail(`${locale}: キーが基準言語と一致しません`);
    const localized: Record<string, Translation> = {};
    for (const e of entries) {
      const raw = entry(doc[e.key], `${locale}/${e.key}`, locale === DEFAULT_LOCALE);
      const texts: string[] = [];
      if (e.plural) {
        if (raw.text !== undefined || !isDict(raw.forms) || !("other" in raw.forms))
          fail(`${locale}/${e.key}: forms.other が必要です`);
        const forms = raw.forms as Dict;
        keys(forms, CATEGORIES, e.key);
        const allowed = new Intl.PluralRules(locale).resolvedOptions().pluralCategories;
        for (const [category, text] of Object.entries(forms)) {
          if (!allowed.includes(category as Intl.LDMLPluralRule)) fail(`${locale}/${e.key}: 不要な複数形 ${category}`);
          texts.push(nonempty(text, `${locale}/${e.key}/${category}`));
        }
        for (const category of allowed)
          if (!(category in forms)) fail(`${locale}/${e.key}: forms.${category} が必要です`);
        localized[e.key] = { forms: forms as Record<string, string> };
      } else {
        if (raw.forms !== undefined) fail(`${locale}/${e.key}: plural のない forms`);
        const text = nonempty(raw.text, `${locale}/${e.key}`);
        texts.push(text);
        localized[e.key] = { text };
      }
      for (const text of texts) {
        const names = [...text.matchAll(/\{([a-zA-Z][a-zA-Z0-9]*)\}/g)].map((m) => m[1]!);
        if (/[{}]/.test(text.replace(/\{([a-zA-Z][a-zA-Z0-9]*)\}/g, "")))
          fail(`${locale}/${e.key}: 不正なプレースホルダー`);
        if (
          [...new Set(names)].sort().join("\0") !==
          e.arguments
            .map((a) => a.name)
            .sort()
            .join("\0")
        )
          fail(`${locale}/${e.key}: 引数が一致しません`);
      }
    }
    translations[locale] = localized;
  }
  return { locales: Object.keys(docs).sort(), entries, translations };
}
function format(text: string, e: Entry, os: "ios" | "android"): string {
  return (e.arguments.length ? text.replace(/%/g, "%%") : text).replace(
    /\{([a-zA-Z][a-zA-Z0-9]*)\}/g,
    (_, name: string) => {
      const a = e.arguments.find((a) => a.name === name)!;
      const i = os === "ios" ? e.iosOrder.indexOf(name) : e.arguments.indexOf(a);
      return `%${i + 1}$${a.type === "string" ? (os === "ios" ? "@" : "s") : os === "ios" ? "lld" : "d"}`;
    },
  );
}
function catalog(model: Model, info: boolean): string {
  const strings: Record<string, unknown> = {};
  for (const e of model.entries.filter((e) => !info || e.iosInfoPlist)) {
    const localizations: Record<string, unknown> = {};
    for (const locale of model.locales) {
      const value = model.translations[locale]![e.key]!;
      const unit = (text: string) => ({ stringUnit: { state: "translated", value: format(text, e, "ios") } });
      localizations[locale] = value.forms
        ? {
            stringUnit: { state: "translated", value: `%#@${e.plural}@` },
            substitutions: {
              [e.plural!]: {
                argNum: e.iosOrder.indexOf(e.plural!) + 1,
                formatSpecifier: "lld",
                variations: {
                  plural: Object.fromEntries(
                    Object.entries(value.forms)
                      .sort()
                      .map(([k, v]) => [k, unit(v)]),
                  ),
                },
              },
            },
          }
        : unit(value.text!);
    }
    strings[info ? e.iosInfoPlist! : e.key] = {
      extractionState: "manual",
      ...(e.comment ? { comment: e.comment } : {}),
      localizations,
    };
  }
  return JSON.stringify({ sourceLanguage: DEFAULT_LOCALE, strings, version: "1.0" }, null, 2) + "\n";
}
function xml(text: string): string {
  // Quoting preserves whitespace and prevents Android treating @ / ? as resource references.
  return (
    '"' +
    text
      .replace(/\\/g, "\\\\")
      .replace(/"/g, '\\"')
      .replace(/'/g, "\\'")
      .replace(/\n/g, "\\n")
      .replace(/\t/g, "\\t")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;") +
    '"'
  );
}
function swift(model: Model): string {
  const out = [
    "// Generated by tools/generate.ts. Do not edit.",
    "import Foundation",
    "",
    "public enum Message: Equatable, Sendable {",
  ];
  for (const e of model.entries)
    out.push(
      `    case ${camel(e.key)}${e.arguments.length ? "(" + e.arguments.map((a) => `${a.name}: ${a.type === "string" ? "String" : "Int"}`).join(", ") + ")" : ""}`,
    );
  out.push("", "    public func resolve(locale: Locale) -> String {", "        switch self {");
  for (const e of model.entries) {
    const t = model.translations[DEFAULT_LOCALE]![e.key]!;
    const source = quote(t.text ?? t.forms!.other!).replace(
      /\{([a-zA-Z][a-zA-Z0-9]*)\}/g,
      (_, n: string) => `\\(${n})`,
    );
    out.push(
      `        case ${e.arguments.length ? "let " : ""}.${camel(e.key)}${e.arguments.length ? "(" + e.arguments.map((a) => a.name).join(", ") + ")" : ""}:`,
    );
    out.push(
      `            String(localized: LocalizedStringResource(${quote(e.key)}, defaultValue: ${source}, locale: LocalizationResources.supportedLocale(locale), bundle: .atURL(LocalizationResources.bundle.bundleURL)))`,
    );
  }
  out.push("        }", "    }", "}", "");
  return out.join("\n");
}
function kotlin(model: Model): string {
  const out = [
    "// Generated by tools/generate.ts. Do not edit.",
    "package com.example.core.localization",
    "",
    "import android.content.Context",
    "",
    "sealed interface Message {",
  ];
  for (const e of model.entries)
    out.push(
      e.arguments.length
        ? `    data class ${cap(camel(e.key))}(${e.arguments.map((a) => `val ${a.name}: ${a.type === "string" ? "String" : "Int"}`).join(", ")}) : Message`
        : `    data object ${cap(camel(e.key))} : Message`,
    );
  out.push("", "    fun resolve(context: Context): String = when (this) {");
  for (const e of model.entries) {
    const args = e.arguments.map((a) => a.name);
    out.push(
      `        ${e.arguments.length ? "is " : ""}${cap(camel(e.key))} -> ${e.plural ? `context.resources.getQuantityString(R.plurals.${e.key}, ${e.plural}` : `context.getString(R.string.${e.key}`}${args.length ? ", " + args.join(", ") : ""})`,
    );
  }
  out.push("    }", "}", "");
  return out.join("\n");
}
/** Build everything before touching any output directory. */
export function outputs(model: Model): Record<string, string> {
  const out: Record<string, string> = {
    [`${IOS}/Message.swift`]: swift(model),
    [`${IOS}/Resources/Localizable.xcstrings`]: catalog(model, false),
    [`${ANDROID}/kotlin/com/example/core/localization/Message.kt`]: kotlin(model),
    "modules/Generated/Localization/config.json":
      JSON.stringify({ defaultLocale: DEFAULT_LOCALE, locales: model.locales }, null, 2) + "\n",
    "apps/ios/App/Generated/Localization/InfoPlist.xcstrings": catalog(model, true),
    "apps/ios/Generated/Localization/config.yml": `# Generated. Do not edit.\noptions:\n  developmentLanguage: ${DEFAULT_LOCALE}\n  knownRegions: ${JSON.stringify(model.locales)}\n`,
    "apps/android/app/src/main/generated/localization/res/resources.properties": `# Generated. Do not edit.\nunqualifiedResLocale=${DEFAULT_LOCALE}\n`,
    "apps/android/app/src/main/generated/localization/locales.txt": model.locales.join("\n") + "\n",
  };
  for (const locale of model.locales) {
    const dir =
      locale === DEFAULT_LOCALE
        ? "values"
        : locale.includes("-")
          ? `values-b+${locale.replace(/-/g, "+")}`
          : `values-${locale}`;
    const rows = [
      '<?xml version="1.0" encoding="utf-8"?>',
      "<!-- Generated by tools/generate.ts. Do not edit. -->",
      "<resources>",
    ];
    for (const e of model.entries) {
      const value = model.translations[locale]![e.key]!;
      if (value.forms) {
        rows.push(`    <plurals name="${e.key}">`);
        for (const [quantity, text] of Object.entries(value.forms).sort())
          rows.push(`        <item quantity="${quantity}">${xml(format(text, e, "android"))}</item>`);
        rows.push("    </plurals>");
      } else
        rows.push(
          `    <string name="${e.key}"${e.arguments.length ? "" : ' formatted="false"'}>${xml(format(value.text!, e, "android"))}</string>`,
        );
    }
    rows.push("</resources>", "");
    out[`${ANDROID}/res/${dir}/strings.xml`] = rows.join("\n");
  }
  return out;
}
export function clean(root: string): void {
  for (const path of ROOTS) rmSync(join(root, path), { recursive: true, force: true });
}
