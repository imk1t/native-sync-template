/**
 * Swift と Kotlin の静的解析をまとめて実行する。CI の lint ジョブと同じ設定・バージョンを使う。
 *   bun tools/lint.ts
 * SwiftLint と ktlint は mise で入れる（mise install）。バージョンは mise.toml を参照。
 * 生成コード（Generated/・generated/）は対象外。
 */
import { ROOT } from "./lib/util";

const tools: [string, string[]][] = [
  ["swiftlint", ["lint", "--strict"]],
  ["ktlint", ["**/*.kt", "**/*.kts", "!**/generated/**", "!**/build/**", "!**/node_modules/**"]],
];

let status = 0;
for (const [name, args] of tools) {
  const bin = Bun.which(name);
  if (!bin) {
    console.error(`${name} が見つかりません（mise install）`);
    status = 1;
    continue;
  }
  const result = Bun.spawnSync([bin, ...args], { cwd: ROOT, stdout: "inherit", stderr: "inherit" });
  if (result.exitCode !== 0) status = 1;
}
process.exit(status);
