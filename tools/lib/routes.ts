/**
 * shared/routes.yaml から Swift / Kotlin の Route と AppTab を生成する（docs/adr/0002）。
 *
 * 未対応の書き方は黙って無視せず Error で失敗する。
 * - routes: name（lowerCamelCase）・feature・params（string / integer）・deepLink
 * - tabs: id（lowerCamelCase）・title・root（引数の無い route）
 * - feature は modules/ 配下の spec.md のあるモジュールで、その spec.md が `name` を参照していること
 */
import { compare, isDict, isFile, join, readText } from "./util";

const IDENT = /^[a-z][A-Za-z0-9]*$/;
const PARAM = /^\{([A-Za-z][A-Za-z0-9]*)\}$/;
const SEGMENT = /^[A-Za-z0-9._~-]+$/;
const TYPES = { string: ["String", "String"], integer: ["Int", "Long"] } as const;
type ParamType = keyof typeof TYPES;

export type Route = {
  name: string;
  feature: string;
  params: [string, ParamType][];
  segments: string[] | null;
};
export type Tab = { id: string; title: string; root: string };
export type RoutesModel = { routes: Route[]; tabs: Tab[] };

function cap(name: string): string {
  return name.charAt(0).toUpperCase() + name.slice(1);
}

function ident(value: unknown, where: string): string {
  if (typeof value !== "string" || !IDENT.test(value)) {
    throw new Error(`${where}: lowerCamelCase の英数字にしてください: ${JSON.stringify(value ?? null)}`);
  }
  return value;
}

function onlyKeys(value: object, allowed: string[]): boolean {
  return Object.keys(value).every((key) => allowed.includes(key));
}

function sameMembers(a: string[], b: string[]): boolean {
  return [...a].sort(compare).join("\0") === [...b].sort(compare).join("\0");
}

/** routes.yaml を検証してモデルを返す。root を渡すと feature と spec.md も検査する。 */
export function load(doc: unknown, root?: string): RoutesModel {
  if (!isDict(doc) || !onlyKeys(doc, ["routes", "tabs"])) {
    throw new Error("routes.yaml は routes と tabs だけを持つ辞書にしてください");
  }
  const rawRoutes = doc.routes;
  if (!Array.isArray(rawRoutes) || rawRoutes.length === 0) throw new Error("routes が必要です");
  const routes: Route[] = [];
  const names = new Set<string>();
  const patterns = new Set<string>();
  rawRoutes.forEach((raw: unknown, index) => {
    const where = `routes[${index}]`;
    if (!isDict(raw) || !onlyKeys(raw, ["name", "feature", "params", "deepLink"])) {
      throw new Error(`${where}: name・feature・params・deepLink だけを書いてください`);
    }
    const name = ident(raw.name, `${where}.name`);
    if (names.has(name)) throw new Error(`${where}.name: 重複しています: ${name}`);
    names.add(name);
    const feature = raw.feature;
    if (typeof feature !== "string" || !feature.startsWith("features/")) {
      throw new Error(`${where}.feature: features/<名前> を書いてください: ${JSON.stringify(feature ?? null)}`);
    }
    if (root !== undefined) {
      const spec = join(root, "modules", feature, "spec.md");
      if (!isFile(spec)) {
        throw new Error(`${where}.feature: spec.md のあるモジュールではありません: ${feature}`);
      }
      if (!readText(spec).includes(`\`${name}\``)) {
        throw new Error(`modules/${feature}/spec.md: 画面遷移の節で \`${name}\` を参照してください`);
      }
    }
    const rawParams = raw.params ?? {};
    if (!isDict(rawParams)) throw new Error(`${where}.params: 引数名をキーにした辞書にしてください`);
    const params: [string, ParamType][] = [];
    for (const [key, kind] of Object.entries(rawParams)) {
      ident(key, `${where}.params`);
      if (kind !== "string" && kind !== "integer") {
        throw new Error(`${where}.params.${key}: string か integer にしてください: ${JSON.stringify(kind ?? null)}`);
      }
      params.push([key, kind]);
    }
    let segments: string[] | null = null;
    if ("deepLink" in raw) {
      const link = raw.deepLink;
      if (typeof link !== "string" || !link.startsWith("/")) {
        throw new Error(`${where}.deepLink: / で始まるパスにしてください: ${JSON.stringify(link ?? null)}`);
      }
      segments = link.split("/").filter((s) => s);
      const used: string[] = [];
      for (const segment of segments) {
        const param = PARAM.exec(segment);
        if (param) used.push(param[1] as string);
        else if (!SEGMENT.test(segment)) {
          throw new Error(`${where}.deepLink: 使えない区切りです: ${JSON.stringify(segment)}`);
        }
      }
      const keys = params.map(([key]) => key);
      if (!sameMembers(used, keys) || new Set(used).size !== used.length) {
        throw new Error(`${where}.deepLink: {引数} は params と 1 対 1 にしてください`);
      }
      const shape = segments.map((s) => (PARAM.test(s) ? "{}" : s)).join("/");
      if (patterns.has(shape)) throw new Error(`${where}.deepLink: 他の route と同じ形です: ${link}`);
      patterns.add(shape);
    }
    routes.push({ name, feature, params, segments });
  });

  const rawTabs = doc.tabs;
  if (!Array.isArray(rawTabs) || rawTabs.length === 0) {
    throw new Error("tabs が必要です（タブを使わない場合も 1 つ書く）");
  }
  const tabs: Tab[] = [];
  const ids = new Set<string>();
  const byName = new Map(routes.map((r) => [r.name, r]));
  rawTabs.forEach((raw: unknown, index) => {
    const where = `tabs[${index}]`;
    if (!isDict(raw) || !sameMembers(Object.keys(raw), ["id", "title", "root"])) {
      throw new Error(`${where}: id・title・root を書いてください`);
    }
    const tabId = ident(raw.id, `${where}.id`);
    if (ids.has(tabId)) throw new Error(`${where}.id: 重複しています: ${tabId}`);
    ids.add(tabId);
    const title = raw.title;
    if (typeof title !== "string" || !title || title.includes('"') || title.includes("\\")) {
      throw new Error(`${where}.title: 空でない文字列にしてください（" と \\ は使えません）`);
    }
    const rootRoute = typeof raw.root === "string" ? byName.get(raw.root) : undefined;
    if (rootRoute === undefined || rootRoute.params.length > 0) {
      throw new Error(`${where}.root: 引数の無い route の name にしてください: ${JSON.stringify(raw.root ?? null)}`);
    }
    tabs.push({ id: tabId, title, root: rootRoute.name });
  });
  return { routes, tabs };
}

/** 引数の少ない（固定の区切りが多い）route から順に照合する。 */
function matchers(routes: Route[]): (Route & { segments: string[] })[] {
  return routes
    .filter((r): r is Route & { segments: string[] } => r.segments !== null)
    .sort((a, b) => a.params.length - b.params.length);
}

function paramType(route: Route, key: string): ParamType | undefined {
  return route.params.find(([k]) => k === key)?.[1];
}

export function swift(model: RoutesModel): string {
  const out = [
    "// Generated by tools/generate.ts from shared/routes.yaml. Do not edit.",
    "import Foundation",
    "",
    "/// shared/routes.yaml の画面。",
    "public enum Route: Hashable, Sendable {",
  ];
  for (const r of model.routes) {
    out.push(`    /// ${r.feature}`);
    if (r.params.length > 0) {
      const args = r.params.map(([k, t]) => `${k}: ${TYPES[t][0]}`).join(", ");
      out.push(`    case ${r.name}(${args})`);
    } else {
      out.push(`    case ${r.name}`);
    }
  }
  out.push(
    "}",
    "",
    "/// shared/routes.yaml のタブ。",
    "public enum AppTab: String, CaseIterable, Hashable, Sendable {",
  );
  out.push(...model.tabs.map((t) => `    case ${t.id}`));
  out.push("", "    /// タブの先頭の画面", "    public var root: Route {", "        switch self {");
  out.push(...model.tabs.map((t) => `        case .${t.id}: .${t.root}`));
  out.push("        }", "    }", "", "    public var title: String {", "        switch self {");
  out.push(...model.tabs.map((t) => `        case .${t.id}: "${t.title}"`));
  out.push(
    "        }",
    "    }",
    "}",
    "",
    "extension Route {",
    "    /// ディープリンクのパス（例: /items/1）から Route を作る。一致しなければ nil。",
    "    public init?(path: String) {",
    '        let segments = path.split(separator: "/").map { String($0).removingPercentEncoding ?? String($0) }',
  );
  for (const r of matchers(model.routes)) {
    const conds = [`segments.count == ${r.segments.length}`];
    const values = new Map<string, string>();
    r.segments.forEach((seg, i) => {
      const param = PARAM.exec(seg)?.[1];
      if (param === undefined) {
        conds.push(`segments[${i}] == "${seg}"`);
      } else if (paramType(r, param) === "integer") {
        conds.push(`let ${param} = Int(segments[${i}])`);
        values.set(param, param);
      } else {
        values.set(param, `segments[${i}]`);
      }
    });
    const args = r.params.map(([k]) => `${k}: ${values.get(k)}`).join(", ");
    const value = r.params.length > 0 ? `.${r.name}(${args})` : `.${r.name}`;
    out.push(`        if ${conds.join(", ")} {`);
    out.push(`            self = ${value}`);
    out.push("            return");
    out.push("        }");
  }
  out.push("        return nil", "    }", "}", "");
  return out.join("\n");
}

export function kotlin(model: RoutesModel, pkg: string): string {
  const out = [
    "// Generated by tools/generate.ts from shared/routes.yaml. Do not edit.",
    `package ${pkg}`,
    "",
    "import java.net.URLDecoder",
    "",
    "/** shared/routes.yaml の画面。 */",
    "sealed interface Route {",
  ];
  for (const r of model.routes) {
    out.push(`    /** ${r.feature} */`);
    if (r.params.length > 0) {
      const args = r.params.map(([k, t]) => `val ${k}: ${TYPES[t][1]}`).join(", ");
      out.push(`    data class ${cap(r.name)}(${args}) : Route`);
    } else {
      out.push(`    data object ${cap(r.name)} : Route`);
    }
  }
  out.push(
    "",
    "    companion object {",
    "        /** ディープリンクのパス（例: /items/1）から Route を作る。一致しなければ null。 */",
    "        fun fromPath(path: String): Route? {",
    '            val segments = path.split("/").filter { it.isNotEmpty() }.map(::decode)',
  );
  for (const r of matchers(model.routes)) {
    const conds = [`segments.size == ${r.segments.length}`];
    const values = new Map<string, string>();
    r.segments.forEach((seg, i) => {
      const param = PARAM.exec(seg)?.[1];
      if (param === undefined) {
        conds.push(`segments[${i}] == "${seg}"`);
      } else if (paramType(r, param) === "integer") {
        conds.push(`segments[${i}].toLongOrNull() != null`);
        values.set(param, `segments[${i}].toLong()`);
      } else {
        values.set(param, `segments[${i}]`);
      }
    });
    const args = r.params.map(([k]) => values.get(k)).join(", ");
    const value = r.params.length > 0 ? `${cap(r.name)}(${args})` : cap(r.name);
    out.push(`            if (${conds.join(" && ")}) return ${value}`);
  }
  out.push(
    "            return null",
    "        }",
    "",
    "        // パーセントエンコードを戻す。不正な値はそのまま使う（iOS の removingPercentEncoding と同じ）。",
    "        private fun decode(segment: String): String =",
    '            runCatching { URLDecoder.decode(segment.replace("+", "%2B"), "UTF-8") }.getOrDefault(segment)',
    "    }",
    "}",
    "",
    "/** shared/routes.yaml のタブ。 */",
    "enum class AppTab(val root: Route, val title: String) {",
  );
  out.push(...model.tabs.map((t) => `    ${cap(t.id)}(Route.${cap(t.root)}, "${t.title}"),`));
  out.push("}", "");
  return out.join("\n");
}
