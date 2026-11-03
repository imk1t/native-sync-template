import { expect } from "bun:test";
import { cpSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { ROOT } from "../lib/util";

/** リポジトリの一部を一時ディレクトリにコピーする。ビルド成果物は除く。 */
export function copyRepo(paths: string[]): { root: string; cleanup: () => void } {
  const root = mkdtempSync(join(tmpdir(), "tools-test-"));
  const skip = new Set([".build", "build", ".gradle"]);
  for (const path of paths) {
    const dest = join(root, path);
    mkdirSync(dirname(dest), { recursive: true });
    cpSync(join(ROOT, path), dest, {
      recursive: true,
      filter: (source) => !skip.has(source.split(/[\\/]/).pop() ?? ""),
    });
  }
  return { root, cleanup: () => rmSync(root, { recursive: true, force: true }) };
}

/** ファイルの中の before を after に置き換える。before が無ければテストを失敗させる。 */
export function replace(root: string, name: string, before: string, after: string, all = false): void {
  const path = join(root, name);
  const text = readFileSync(path, "utf-8");
  expect(text).toContain(before);
  writeFileSync(path, all ? text.replaceAll(before, after) : text.replace(before, after));
}

/** main(root, log) を実行し、終了コードと出力を返す。 */
export function capture(main: (root: string, log: (line: string) => void) => number, root: string) {
  const lines: string[] = [];
  const code = main(root, (line) => lines.push(line));
  return { code, output: lines.join("\n") };
}
