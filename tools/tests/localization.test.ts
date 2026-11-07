import { afterEach, beforeEach, expect, test } from "bun:test";
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { stringify } from "yaml";
import { generate, OUTPUTS } from "../generate";
import { ANDROID, IOS, load, outputs } from "../lib/localization";
import { copyRepo } from "./helpers";

let root: string;
beforeEach(() => {
  root = mkdtempSync(join(tmpdir(), "localization-"));
  mkdirSync(join(root, "specs/localization"), { recursive: true });
});
afterEach(() => rmSync(root, { recursive: true, force: true }));
const write = (locale: string, data: unknown) =>
  writeFileSync(join(root, `specs/localization/messages-${locale}.yaml`), stringify(data));
const simple = () => {
  write("ja", { greeting: "こんにちは" });
  write("en", { greeting: "Hello" });
};

test("all languages must have the base keys and translations, without repeated YAML keys", () => {
  simple();
  write("en", { other: "Hello" });
  expect(() => load(root)).toThrow("キー");
  write("en", { greeting: "" });
  expect(() => load(root)).toThrow("空でない");
  writeFileSync(join(root, "specs/localization/messages-en.yaml"), "greeting: Hello\ngreeting: Hi\n");
  expect(() => load(root)).toThrow("unique");
  simple();
  rmSync(join(root, "specs/localization/messages-ja.yaml"));
  expect(() => load(root)).toThrow("messages-ja");
});

test("invalid tags, metadata and generated identifiers fail", () => {
  simple();
  write("en_US", { greeting: "Hello" });
  expect(() => load(root)).toThrow("ファイル名");
  rmSync(join(root, "specs/localization/messages-en_US.yaml"));
  write("en", { greeting: { text: "Hello", arguments: [] } });
  expect(() => load(root)).toThrow("未対応");
  write("ja", { foo_1: "a", foo1: "b" });
  expect(() => load(root)).toThrow("識別子");
  write("ja", { class: "a" });
  expect(() => load(root)).toThrow("予約語");
});

test("argument reordering follows the source interpolation order on iOS and declaration order on Android", () => {
  write("ja", {
    message: {
      arguments: [
        { name: "first", type: "string" },
        { name: "second", type: "integer" },
      ],
      text: "{second} / {first}",
    },
  });
  write("en", { message: { text: "{first}: {second}" } });
  const files = outputs(load(root));
  const catalog = JSON.parse(files[`${IOS}/Resources/Localizable.xcstrings`]!);
  expect(catalog.strings.message.localizations.en.stringUnit.value).toBe("%2$@: %1$lld");
  expect(files[`${ANDROID}/res/values-en/strings.xml`]).toContain("%1$s: %2$,d");
  write("en", { message: "{second}" });
  expect(() => load(root)).toThrow("引数");
  write("en", { message: "{first}: {extra}" });
  expect(() => load(root)).toThrow("引数");
});

test("plural selection uses the declared integer, even after a string argument", () => {
  write("ja", {
    message: {
      arguments: [
        { name: "name", type: "string" },
        { name: "count", type: "integer" },
      ],
      plural: "count",
      forms: { other: "{name}: {count}件" },
    },
  });
  write("en", { message: { forms: { one: "{name}: {count} result", other: "{count} results for {name}" } } });
  const files = outputs(load(root));
  const catalog = JSON.parse(files[`${IOS}/Resources/Localizable.xcstrings`]!);
  expect(catalog.strings.message.localizations.en.substitutions.count.argNum).toBe(2);
  expect(files[`${ANDROID}/kotlin/com/example/core/localization/Message.kt`]).toContain(
    "getQuantityString(R.plurals.message, count, name, count)",
  );
  write("en", { message: { forms: { other: "{count} results for {name}" } } });
  expect(() => load(root)).toThrow("forms.one");
  write("en", { message: { forms: { one: "{count} result for {name}" } } });
  expect(() => load(root)).toThrow("forms.other");
});

test("quotes, percent signs, whitespace, XML and resource-reference characters survive generation", () => {
  const text = "@value & <tag> \"quoted\" 'apostrophe' 100%\nnext\\line";
  write("ja", { message: text });
  write("en", { message: text });
  const files = outputs(load(root));
  expect(
    JSON.parse(files[`${IOS}/Resources/Localizable.xcstrings`]!).strings.message.localizations.en.stringUnit.value,
  ).toBe(text);
  const xml = files[`${ANDROID}/res/values-en/strings.xml`]!;
  expect(xml).toContain('formatted="false"');
  expect(xml).toContain('&amp; &lt;tag&gt; \\"quoted\\"');
  expect(xml).toContain("100%\\nnext\\\\line");
});

test("adding and removing a language updates metadata and removes only owned outputs", () => {
  const repo = copyRepo([
    "specs",
    "DESIGN.md",
    "modules/features/search/spec.md",
    "modules/features/bookmarks/spec.md",
    "modules/features/settings/spec.md",
  ]);
  try {
    generate(repo.root);
    const directory = join(repo.root, "specs/localization");
    writeFileSync(join(directory, "messages-de.yaml"), readFileSync(join(directory, "messages-en.yaml")));
    generate(repo.root);
    expect(existsSync(join(repo.root, ANDROID, "res/values-de/strings.xml"))).toBe(true);
    expect(
      readFileSync(join(repo.root, "apps/android/app/src/main/generated/localization/locales.txt"), "utf8"),
    ).toContain("de\n");
    rmSync(join(directory, "messages-de.yaml"));
    generate(repo.root);
    expect(existsSync(join(repo.root, ANDROID, "res/values-de/strings.xml"))).toBe(false);
    const before = readFileSync(join(repo.root, OUTPUTS.iosTokens), "utf8");
    writeFileSync(join(directory, "messages-en.yaml"), "invalid: value\n");
    expect(() => generate(repo.root)).toThrow();
    expect(readFileSync(join(repo.root, OUTPUTS.iosTokens), "utf8")).toBe(before);
    expect(existsSync(join(repo.root, IOS, "Message.swift"))).toBe(true);
  } finally {
    repo.cleanup();
  }
});

test("app metadata is translated in both its native table and the shared table", () => {
  write("ja", { app_label: { text: "アプリ", iosInfoPlist: "CFBundleDisplayName" } });
  write("en", { app_label: "App" });
  const files = outputs(load(root));
  const metadata = JSON.parse(files["apps/ios/App/Generated/Localization/InfoPlist.xcstrings"]!);
  expect(metadata.strings.CFBundleDisplayName.localizations.en.stringUnit.value).toBe("App");
  const catalog = JSON.parse(files[`${IOS}/Resources/Localizable.xcstrings`]!);
  expect(catalog.strings.app_label.localizations.en.stringUnit.value).toBe("App");
});
