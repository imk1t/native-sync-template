import { existsSync, readFileSync, statSync } from "node:fs";
import { join, resolve } from "node:path";

/** リポジトリのルート（tools/ の親）。 */
export const ROOT = resolve(import.meta.dir, "../..");

export type Dict = Record<string, unknown>;

export function isDict(value: unknown): value is Dict {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function readText(path: string): string {
  return readFileSync(path, "utf-8");
}

export function isFile(path: string): boolean {
  return existsSync(path) && statSync(path).isFile();
}

export function isDir(path: string): boolean {
  return existsSync(path) && statSync(path).isDirectory();
}

export function readYaml(path: string): unknown {
  return Bun.YAML.parse(readText(path));
}

/** 文字列をコードポイント順に比べる（ロケールに依存しない決定的な順序）。 */
export function compare(a: string, b: string): number {
  return a < b ? -1 : a > b ? 1 : 0;
}

/** 文字列の配列を辞書順に比べる。 */
export function compareParts(a: readonly string[], b: readonly string[]): number {
  for (let i = 0; i < Math.min(a.length, b.length); i++) {
    const c = compare(a[i] as string, b[i] as string);
    if (c !== 0) return c;
  }
  return a.length - b.length;
}

export function sorted(values: Iterable<string>): string[] {
  return [...values].sort(compare);
}

/** glob に一致するパス（cwd からの相対パス、/ 区切り）を返す。 */
export function glob(cwd: string, pattern: string, options: { dot?: boolean; onlyFiles?: boolean } = {}): string[] {
  if (!isDir(cwd)) return [];
  const scan = new Bun.Glob(pattern).scanSync({
    cwd,
    dot: options.dot ?? false,
    onlyFiles: options.onlyFiles ?? true,
  });
  return [...scan].map((p) => p.split("\\").join("/")).sort(compare);
}

export { join };
