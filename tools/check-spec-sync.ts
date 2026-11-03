/**
 * 各モジュールの spec.md と、ios/・android/ の .spec-sync（追従済み spec のハッシュ）を照合する。
 *
 *   bun tools/check-spec-sync.ts           ズレがあれば失敗（CI 用）
 *   bun tools/check-spec-sync.ts --update ios|android|both <module-path>...   指定モジュールの追従を記録する
 */
import { existsSync, writeFileSync } from "node:fs";
import { relative, resolve } from "node:path";
import { androidModules, iosTargets } from "./lib/manifest";
import { compareParts, glob, isDir, isFile, join, readText, ROOT } from "./lib/util";

const PLATFORMS = ["ios", "android"] as const;
type Platform = (typeof PLATFORMS)[number];

export function specHash(path: string): string {
  const text = readText(path).replace(/\r\n?/g, "\n");
  return new Bun.CryptoHasher("sha256").update(text).digest("hex").slice(0, 16);
}

/** 検査するモジュール（modules/ からの相対パス）。spec を消しても、ビルド定義や実装ディレクトリに残るものを含める。 */
function specs(root: string): string[] {
  const modules = join(root, "modules");
  const found = new Set<string>();
  const parent = (path: string) => path.split("/").slice(0, -1).join("/");
  for (const path of glob(modules, "**/spec.md", { dot: true })) found.add(parent(path));
  for (const { module } of iosTargets(root).values()) found.add(module);
  for (const module of androidModules(root).values()) found.add(module);
  for (const plat of PLATFORMS) {
    for (const path of glob(modules, `**/${plat}`, { onlyFiles: false })) {
      if (isDir(join(modules, path))) found.add(parent(path));
    }
  }
  return [...found].sort((a, b) => compareParts(a.split("/"), b.split("/")));
}

export function check(root: string = ROOT): string[] {
  const errors: string[] = [];
  for (const mod of specs(root)) {
    const dir = join(root, "modules", mod);
    const rel = `modules/${mod}`;
    if (!isFile(join(dir, "spec.md"))) {
      errors.push(`${rel}: spec.md がありません`);
      continue;
    }
    const expected = specHash(join(dir, "spec.md"));
    for (const plat of PLATFORMS) {
      const marker = join(dir, plat, ".spec-sync");
      if (!isDir(join(dir, plat))) errors.push(`${rel}: ${plat}/ がありません`);
      else if (!existsSync(marker)) errors.push(`${rel}: ${plat}/.spec-sync がありません（追従後に --update で記録）`);
      else if (readText(marker).trim() !== expected) errors.push(`${rel}: ${plat} が spec.md の最新に追従していません`);
    }
  }
  return errors;
}

export function update(plats: readonly Platform[], paths: string[], root: string = ROOT): void {
  for (const path of paths) {
    const mod = resolve(root, path);
    if (!existsSync(join(mod, "spec.md"))) {
      console.error(`spec.md がありません: ${path}`);
      process.exit(1);
    }
    for (const plat of plats) {
      writeFileSync(join(mod, plat, ".spec-sync"), `${specHash(join(mod, "spec.md"))}\n`);
      console.log(`updated ${relative(root, mod)}/${plat}/.spec-sync`);
    }
  }
}

if (import.meta.main) {
  const args = process.argv.slice(2);
  if (args[0] === "--update") {
    const target = args[1];
    if (args.length < 3 || (target !== "ios" && target !== "android" && target !== "both")) {
      console.error("usage: --update ios|android|both <module-path>...");
      process.exit(1);
    }
    update(target === "both" ? PLATFORMS : [target], args.slice(2));
    process.exit(0);
  }
  let errors: string[];
  try {
    errors = check();
  } catch (error) {
    console.error(`ERROR ${error instanceof Error ? error.message : error}`);
    process.exit(1);
  }
  for (const e of errors) console.log(`ERROR ${e}`);
  console.log(errors.length === 0 ? "spec sync: OK" : `spec sync: ${errors.length} problem(s)`);
  process.exit(errors.length === 0 ? 0 : 1);
}
