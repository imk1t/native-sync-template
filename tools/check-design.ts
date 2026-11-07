/** 公式の DESIGN.md lint（@google/design.md）を実行し、エラーと警告があれば失敗にする。 */
import { join } from "node:path";
import { ROOT } from "./lib/util";

interface Finding {
  severity: "error" | "warning" | "info";
  path?: string;
  message: string;
  rule: string;
}

export function main(root: string = ROOT): number {
  const result = Bun.spawnSync(["bun", "x", "designmd", "lint", join(root, "DESIGN.md")], { cwd: root });
  const output = result.stdout.toString();
  let findings: Finding[];
  try {
    findings = (JSON.parse(output) as { findings: Finding[] }).findings;
  } catch {
    console.log(output + result.stderr.toString());
    console.log("DESIGN.md の lint 結果を読めませんでした");
    return 1;
  }
  const problems = findings.filter((finding) => finding.severity !== "info");
  for (const finding of problems) {
    console.log(`DESIGN.md ${finding.path ?? ""}: [${finding.severity}] ${finding.message} (${finding.rule})`);
  }
  if (problems.length > 0) return 1;
  console.log("design: OK");
  return result.exitCode === 0 ? 0 : 1;
}

if (import.meta.main) process.exit(main());
