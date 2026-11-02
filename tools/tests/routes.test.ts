/** shared/routes.yaml の検証と生成を確かめる。 */
import { describe, expect, test } from "bun:test";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import * as routes from "../lib/routes";
import { type Dict, readYaml, ROOT } from "../lib/util";

const BASE = {
  tabs: [{ id: "home", title: "ホーム", root: "items" }],
  routes: [
    { name: "items", feature: "features/items", deepLink: "/items" },
    { name: "itemDetail", feature: "features/items", params: { id: "string" }, deepLink: "/items/{id}" },
  ],
};

type Doc = { tabs: Dict[]; routes: Dict[] };

function doc(change: (value: Doc) => void): Doc {
  const value = structuredClone(BASE) as Doc;
  change(value);
  return value;
}

describe("routes", () => {
  test("Swift と Kotlin で同じ構造を生成する", () => {
    const model = routes.load(BASE);
    const swift = routes.swift(model);
    const kotlin = routes.kotlin(model, "com.example.corenavigation");
    expect(swift).toContain("case itemDetail(id: String)");
    expect(kotlin).toContain("data class ItemDetail(val id: String) : Route");
    expect(swift).toContain('case .home: "ホーム"');
    expect(kotlin).toContain('Home(Route.Items, "ホーム"),');
  });

  test("固定の区切りを引数より先に照合する", () => {
    const model = routes.load(
      doc((d) => {
        d.routes.unshift({
          name: "itemEdit",
          feature: "features/items",
          params: { id: "string" },
          deepLink: "/items/{id}/edit",
        });
        d.routes.push({ name: "newItem", feature: "features/items", deepLink: "/items/new" });
      }),
    );
    const swift = routes.swift(model);
    expect(swift.indexOf('segments[1] == "new"')).toBeLessThan(swift.indexOf("self = .itemDetail"));
  });

  test("integer の引数", () => {
    const model = routes.load(
      doc((d) => {
        (d.routes[1] as Dict).params = { id: "integer" };
      }),
    );
    expect(routes.swift(model)).toContain("let id = Int(segments[1])");
    expect(routes.kotlin(model, "p")).toContain("ItemDetail(segments[1].toLong())");
    expect(routes.kotlin(model, "p")).toContain("val id: Long");
  });

  const set = (index: number, values: Dict) => (d: Doc) => Object.assign(d.routes[index] as Dict, values);
  const setTab = (values: Dict) => (d: Doc) => Object.assign(d.tabs[0] as Dict, values);
  test.each([
    ["重複", (d: Doc) => d.routes.push({ ...(d.routes[0] as Dict) })],
    ["引数の型", set(1, { params: { id: "object" } })],
    ["deepLink と params", set(1, { deepLink: "/items/{other}" })],
    [
      "同じ形のパス",
      (d: Doc) =>
        d.routes.push({
          name: "other",
          feature: "features/items",
          params: { key: "string" },
          deepLink: "/items/{key}",
        }),
    ],
    ["引数のあるタブの先頭", setTab({ root: "itemDetail" })],
    ["存在しないタブの先頭", setTab({ root: "missing" })],
    ["features 以外", set(0, { feature: "core/network" })],
    ["名前の形", set(0, { name: "Items" })],
    ["未知のキー", set(0, { presentation: "sheet" })],
  ])("不正な定義は失敗する: %s", (_, change) => {
    expect(() => routes.load(doc(change))).toThrow();
  });

  test("feature の spec.md が route を参照していること", () => {
    const root = mkdtempSync(join(tmpdir(), "routes-test-"));
    try {
      const spec = join(root, "modules/features/items/spec.md");
      mkdirSync(join(spec, ".."), { recursive: true });
      writeFileSync(spec, "`items`\n");
      expect(() => routes.load(BASE, root)).toThrow("itemDetail");
      writeFileSync(spec, "`items` `itemDetail`\n");
      routes.load(BASE, root);
    } finally {
      rmSync(root, { recursive: true, force: true });
    }
  });

  test("リポジトリの routes.yaml が正しい", () => {
    routes.load(readYaml(join(ROOT, "shared/routes.yaml")), ROOT);
  });
});
