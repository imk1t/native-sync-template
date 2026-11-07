/** DESIGN.md の front matter 読み込みと検証を確かめる。 */
import { describe, expect, test } from "bun:test";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { main as checkDesign } from "../check-design";
import * as design from "../lib/design";
import { readText, ROOT } from "../lib/util";

const BASE = `---
colors:
  primary: "#3D4DFF"
  primary-dark: "#8C98FF"
rounded:
  card: 22px
spacing:
  small: 8
typography:
  body:
    fontFamily: Inter Tight
    fontSize: 15px
    fontWeight: 400
---
# body
`;

function fail(text: string): string {
  try {
    design.parse(text);
  } catch (error) {
    return (error as Error).message;
  }
  throw new Error("失敗するはずです");
}

describe("DESIGN.md", () => {
  test("実際の DESIGN.md を読める", () => {
    const tokens = design.parse(readText(join(ROOT, "DESIGN.md")));
    expect(tokens.colors.map(([name]) => name)).toContain("primary");
    expect(tokens.rounded.map(([name]) => name)).toContain("card");
  });

  test("色と寸法を名前順に読む", () => {
    expect(design.parse(BASE)).toEqual({
      colors: [["primary", "#3D4DFF", "#8C98FF"]],
      rounded: [["card", 22]],
      spacing: [["small", 8]],
      typography: [["body", 15, 400]],
    });
  });

  test("暗い値のない色は失敗する", () => {
    expect(fail(BASE.replace('  primary-dark: "#8C98FF"\n', ""))).toContain("colors.primary-dark");
  });

  test("ライトのない暗い値は失敗する", () => {
    expect(
      fail(BASE.replace('  primary-dark: "#8C98FF"', '  primary-dark: "#8C98FF"\n  extra-dark: "#000000"')),
    ).toContain("colors.extra");
  });

  test("色は #RRGGBB だけ受け付ける", () => {
    expect(fail(BASE.replace('"#3D4DFF"', '"blue"'))).toContain("#RRGGBB");
  });

  test("寸法は数値と px / dp だけ受け付ける", () => {
    expect(fail(BASE.replace("22px", "2rem"))).toContain("rounded.card");
  });

  test("文字サイズと太さを検証する", () => {
    expect(fail(BASE.replace("15px", "big"))).toContain("typography.body.fontSize");
    expect(fail(BASE.replace("fontWeight: 400", "fontWeight: 450"))).toContain("typography.body.fontWeight");
  });

  test("front matter がないと失敗する", () => {
    expect(fail("# no front matter")).toContain("front matter");
  });
});

describe("DESIGN.md lint", () => {
  test("公式 lint がエラーも警告も出さない", () => {
    expect(checkDesign()).toBe(0);
  });
});

describe("DESIGN.md lint の依存", () => {
  // zod を 4 系に上げて使っているため、lint 自体が壊れていないかを壊れた参照で確かめる。
  test("壊れたトークン参照をエラーにする", () => {
    const directory = mkdtempSync(join(tmpdir(), "design-lint-"));
    try {
      const path = join(directory, "DESIGN.md");
      writeFileSync(
        path,
        '---\ncolors:\n  primary: "#3D4DFF"\ncomponents:\n  button:\n    backgroundColor: "{colors.nope}"\n---\n# x\n',
      );
      const result = Bun.spawnSync(["bun", "x", "designmd", "lint", path], { cwd: ROOT });
      expect(result.exitCode).toBe(1);
      expect(result.stdout.toString()).toContain("broken-ref");
    } finally {
      rmSync(directory, { recursive: true, force: true });
    }
  });
});
