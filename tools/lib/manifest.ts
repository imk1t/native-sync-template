/** ビルド定義からモジュール一覧を読む。未対応の構文は黙って無視せず失敗する。 */
import { join, readText } from "./util";

function modulePath(value: string): string {
  const parts = value.split("/").filter((p) => p !== "" && p !== ".");
  if (value.startsWith("/") || parts.includes("..") || parts.length === 0) {
    throw new Error(`不正なモジュールパス: ${value}`);
  }
  return parts.join("/");
}

export type IosTarget = { module: string; dependencies: string };

/** modules/Package.swift の .target 行を target 名 → { module, dependencies } で返す。 */
export function iosTargets(root: string): Map<string, IosTarget> {
  const targets = new Map<string, IosTarget>();
  const text = readText(join(root, "modules/Package.swift"));
  const row =
    /^\s*\.target\(\s*name:\s*"([^"]+)"\s*,\s*(?:dependencies:\s*\[(.*)\]\s*,\s*)?path:\s*"([^"]+)"\s*\)\s*,?\s*$/;
  for (const line of text.split(/\r?\n/)) {
    if (!line.trim().startsWith(".target(")) continue;
    const match = row.exec(line);
    if (!match) {
      throw new Error(`iOS target は name・dependencies・path の順に1行で記述してください: ${line.trim()}`);
    }
    const [, name = "", deps = "", path = ""] = match;
    const index = path.indexOf("/ios/");
    if (index < 0) throw new Error(`iOS target の path に /ios/ がありません: ${path}`);
    if (targets.has(name)) throw new Error(`重複した iOS target: ${name}`);
    targets.set(name, { module: modulePath(path.slice(0, index)), dependencies: deps });
  }
  if (targets.size === 0) throw new Error("iOS target がありません");
  return targets;
}

/** apps/android/settings.gradle.kts の module(...) 行を Gradle パス → モジュールパスで返す。 */
export function androidModules(root: string): Map<string, string> {
  const text = readText(join(root, "apps/android/settings.gradle.kts"));
  const modules = new Map<string, string>();
  for (const line of text.split(/\r?\n/)) {
    if (!line.trim().startsWith("module(")) continue;
    const match = /^\s*module\("(:[\w:]+)",\s*"([^"]+)"\)\s*$/.exec(line);
    if (!match) throw new Error(`Android module は1行で記述してください: ${line.trim()}`);
    const [, name = "", path = ""] = match;
    if (modules.has(name) || [...modules.values()].includes(path)) {
      throw new Error(`重複した Android module: ${name}, ${path}`);
    }
    modules.set(name, modulePath(path));
  }
  if (modules.size === 0) throw new Error("Android module がありません");
  return modules;
}
