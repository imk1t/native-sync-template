/**
 * モジュール間の依存ルールを検査する。
 *
 *   core/utils          -> 何にも依存しない
 *   core/*              -> 他の core/* のみ（循環禁止）
 *   shared/designsystem -> core/utils のみ
 *   その他の shared/*   -> core/*（shared 同士は禁止）
 *   features/*          -> shared/*, core/*（features 同士は禁止）
 */
import { androidModules, iosTargets } from "./lib/manifest";
import { compare, glob, isDict, join, readText, readYaml, ROOT, sorted } from "./lib/util";

export type Graph = Map<string, Set<string>>;
/** 外部依存。`<モジュール>\t<名前>` の集合。 */
export type External = Set<string>;

const pair = (mod: string, name: string) => `${mod}\t${name}`;
const unpair = (value: string) => value.split("\t") as [string, string];

export function iosDependencies(root: string = ROOT): [Graph, External] {
  const targets = iosTargets(root);
  const deps: Graph = new Map();
  const external: External = new Set();
  const token =
    /^\s*(?:"(?<plain>[^"]+)"|\.target\(\s*name:\s*"(?<target>[^"]+)"\s*\)|\.product\(\s*name:\s*"[^"]+"\s*,\s*package:\s*"(?<package>[^"]+)"\s*\))\s*(?:,|$)/;
  for (const { module: mod, dependencies } of targets.values()) {
    if (deps.has(mod)) throw new Error(`1モジュールに複数の iOS target があります: ${mod}`);
    const found = new Set<string>();
    deps.set(mod, found);
    let raw = dependencies;
    while (raw.trim()) {
      const match = token.exec(raw);
      if (!match?.groups) throw new Error(`${mod}: 未対応の iOS 依存構文: ${raw}`);
      const { plain, target, package: pkg } = match.groups;
      if (pkg) {
        external.add(pair(mod, pkg.toLowerCase()));
      } else {
        const name = (plain ?? target) as string;
        const dependency = targets.get(name);
        if (!dependency) {
          throw new Error(
            `${mod}: 不明な内部 target ${name}。外部依存は .product(name: ..., package: ...) を使ってください`,
          );
        }
        found.add(dependency.module);
      }
      raw = raw.slice(match[0].length);
    }
  }
  return [deps, external];
}

export function androidDependencies(root: string = ROOT): [Graph, External] {
  const modules = join(root, "modules");
  const registered = androidModules(root);
  const deps: Graph = new Map();
  const external: External = new Set();
  const toml = Bun.TOML.parse(readText(join(root, "apps/android/gradle/libs.versions.toml")));
  const catalog = isDict(toml) && isDict(toml.libraries) ? toml.libraries : {};
  const actual = glob(modules, "**/android/build.gradle.kts", { dot: true }).map((p) =>
    p.split("/").slice(0, -2).join("/"),
  );
  const expected = sorted(registered.values());
  if (sorted(new Set(actual)).join("\0") !== sorted(new Set(expected)).join("\0")) {
    throw new Error(
      `Android の登録と build.gradle.kts が一致しません: registered=${JSON.stringify(expected)}, actual=${JSON.stringify(sorted(actual))}`,
    );
  }
  for (const mod of registered.values()) {
    const text = readText(join(modules, mod, "android/build.gradle.kts")).replace(/\/\/[^\n]*|\/\*[\s\S]*?\*\//g, "");
    const found = [...text.matchAll(/project\(\s*(?:path\s*=\s*)?"(:[\w:]+)"\s*\)/g)].map((m) => m[1] as string);
    if (text.split("project(").length - 1 !== found.length) throw new Error(`${mod}: 未対応の project 依存構文`);
    const unknown = sorted(new Set(found.filter((p) => !registered.has(p))));
    if (unknown.length > 0) throw new Error(`${mod}: 不明な Android project: ${JSON.stringify(unknown)}`);
    deps.set(mod, new Set(found.map((p) => registered.get(p) as string)));
    if (/(?:api|implementation|compileOnly|runtimeOnly)\s*\(\s*(?:platform\(\s*)?["']/.test(text)) {
      throw new Error(`${mod}: 外部依存は libs.versions.toml に定義してください`);
    }
    const accessors = text.matchAll(
      /(?:api|implementation|compileOnly|runtimeOnly)\s*\(\s*(?:platform\(\s*)?libs\.([\w.]+)/g,
    );
    for (const [, accessor = ""] of accessors) {
      const alias = accessor.replaceAll(".", "-");
      const library = catalog[alias];
      if (!isDict(library)) throw new Error(`${mod}: 不明な version catalog alias: ${alias}`);
      const coordinate = String(library.module);
      // OS・言語の公式ライブラリは UI / coroutine のために各層で利用できる。
      const official = ["androidx.", "org.jetbrains.kotlin:", "org.jetbrains.kotlinx:"].some((prefix) =>
        coordinate.startsWith(prefix),
      );
      if (mod !== "core/utils" && official) continue;
      external.add(pair(mod, alias));
    }
  }
  return [deps, external];
}

function nonEmptyStrings(value: unknown, keys: string[]): boolean {
  return isDict(value) && keys.every((key) => typeof value[key] === "string" && value[key] !== "");
}

export function checkExternal(used: { ios: External; android: External }, root: string = ROOT): string[] {
  const document = readYaml(join(root, "specs/third-party.yaml"));
  if (!isDict(document) || !Array.isArray(document.libraries)) {
    throw new Error("third-party.yaml の libraries は配列にしてください");
  }
  const declared = { ios: new Set<string>(), android: new Set<string>() };
  for (const entry of document.libraries as unknown[]) {
    if (!isDict(entry) || !nonEmptyStrings(entry, ["role", "module", "owner"])) {
      throw new Error("外部ライブラリには role・module・owner を指定してください");
    }
    for (const plat of ["ios", "android"] as const) {
      const library = entry[plat];
      if (!isDict(library) || !nonEmptyStrings(library, ["name", "license"])) {
        throw new Error(`${entry.module}: ${plat} の name・license が必要です`);
      }
      const name = String(library.name);
      declared[plat].add(pair(String(entry.module), plat === "ios" ? name.toLowerCase() : name));
    }
  }
  const errors: string[] = [];
  for (const plat of ["ios", "android"] as const) {
    const dependencies = used[plat];
    for (const [mod, name] of sorted(dependencies).map(unpair)) {
      if (!mod.startsWith("core/") || mod === "core/utils") {
        errors.push(`${plat}: ${mod} は外部 SDK ${name} に直接依存できません`);
      }
    }
    for (const [mod, name] of sorted([...dependencies].filter((d) => !declared[plat].has(d))).map(unpair)) {
      errors.push(`${plat}: ${mod} の ${name} が third-party.yaml に未登録です`);
    }
    for (const [mod, name] of sorted([...declared[plat]].filter((d) => !dependencies.has(d))).map(unpair)) {
      errors.push(`${plat}: third-party.yaml の ${mod} / ${name} が実際の依存にありません`);
    }
  }
  return errors;
}

export function allowed(src: string, dst: string): boolean {
  if (src === dst || src === "core/utils") return false;
  if (src.startsWith("core/")) return dst.startsWith("core/");
  if (src === "shared/designsystem") return dst === "core/utils";
  if (src.startsWith("shared/")) return dst.startsWith("core/");
  if (src.startsWith("features/")) {
    return dst.startsWith("shared/") || dst.startsWith("core/");
  }
  return false;
}

export function hasCycle(graph: Graph): boolean {
  const state = new Map<string, 1 | 2>();
  const visit = (node: string): boolean => {
    if (state.get(node) === 1) return true;
    if (state.get(node) === 2) return false;
    state.set(node, 1);
    for (const next of graph.get(node) ?? []) if (visit(next)) return true;
    state.set(node, 2);
    return false;
  };
  return [...graph.keys()].some(visit);
}

function describe(graph: Graph): string {
  const entries = [...graph.entries()].sort(([a], [b]) => compare(a, b));
  return JSON.stringify(Object.fromEntries(entries.map(([k, v]) => [k, sorted(v)])));
}

export function main(root: string = ROOT, log: (line: string) => void = console.log): number {
  const errors: string[] = [];
  let graphs: { ios: Graph; android: Graph };
  try {
    const [ios, iosExternal] = iosDependencies(root);
    const [android, androidExternal] = androidDependencies(root);
    errors.push(...checkExternal({ ios: iosExternal, android: androidExternal }, root));
    graphs = { ios, android };
  } catch (error) {
    log(`ERROR ${error instanceof Error ? error.message : error}`);
    return 1;
  }
  for (const plat of ["ios", "android"] as const) {
    const graph = graphs[plat];
    for (const src of sorted(graph.keys())) {
      for (const dst of sorted(graph.get(src) ?? [])) {
        if (!allowed(src, dst)) errors.push(`${plat}: ${src} は ${dst} に依存できません`);
      }
    }
    if (hasCycle(graph)) errors.push(`${plat}: モジュール間に循環依存があります`);
  }
  if (describe(graphs.ios) !== describe(graphs.android)) {
    errors.push(
      `iOS と Android の依存関係が一致しません: ios=${describe(graphs.ios)} android=${describe(graphs.android)}`,
    );
  }
  for (const e of errors) log(`ERROR ${e}`);
  log(errors.length === 0 ? "deps: OK" : `deps: ${errors.length} problem(s)`);
  return errors.length === 0 ? 0 : 1;
}

if (import.meta.main) process.exit(main());
