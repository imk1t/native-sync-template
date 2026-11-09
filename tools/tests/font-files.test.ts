/** core/font の同梱書体が、spec.md に書いたハッシュと一致し、iOS と Android で同じファイルかを確かめる。 */
import { describe, expect, test } from "bun:test";
import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { join, ROOT } from "../lib/util";

const MODULE = join(ROOT, "modules/core/font");
const spec = readFileSync(join(MODULE, "spec.md"), "utf-8");

const sha256 = (path: string) => createHash("sha256").update(readFileSync(path)).digest("hex");

/** spec.md の「Bundled file」表の行から、[ファイル名, Android のリソース名, SHA-256] を読む。 */
function bundled(): [string, string, string][] {
  const rows = [...spec.matchAll(/^\| `([^`]+\.ttf)` \| `([a-z_]+)` \| `([0-9a-f]{64})` \|$/gm)];
  return rows.map((row): [string, string, string] => [row[1] as string, row[2] as string, row[3] as string]);
}

describe("core/font の同梱書体", () => {
  test("spec.md に 4 つの書体ファイルが書かれている", () => {
    expect(bundled().map(([file]) => file)).toEqual([
      "InterTight-Regular.ttf",
      "InterTight-Medium.ttf",
      "InterTight-Bold.ttf",
      "JetBrainsMono-Medium.ttf",
    ]);
  });

  test("iOS と Android のファイルが spec.md のハッシュと一致する", () => {
    for (const [file, resource, hash] of bundled()) {
      expect(sha256(join(MODULE, "ios/Sources/Core/Font/Resources", file))).toBe(hash);
      expect(sha256(join(MODULE, "android/src/main/res/font", `${resource}.ttf`))).toBe(hash);
    }
  });

  test("ライセンス本文が同梱されている", () => {
    for (const name of ["OFL-InterTight.txt", "OFL-JetBrainsMono.txt"]) {
      expect(readFileSync(join(MODULE, "licenses", name), "utf-8")).toContain("SIL Open Font License, Version 1.1");
    }
  });
});
