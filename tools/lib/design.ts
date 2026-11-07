/** DESIGN.md の front matter からデザイントークンを読む。色の暗い値は同じ `colors` に `<名前>-dark` で書く（DESIGN.md 仕様にモードはないため）。 */
import { compare, type Dict, isDict } from "./util";

export interface Tokens {
  /** 名前 → [ライトの値, 暗いときの値]（`#RRGGBB`）。名前順。 */
  colors: [string, string, string][];
  rounded: [string, number][];
  spacing: [string, number][];
  /** 名前 → [文字サイズ, 太さ(100〜900)]。名前順。書体は生成しない（OS の書体に差し替えられるため）。 */
  typography: [string, number, number][];
}

const COLOR = /^#[0-9A-Fa-f]{6}$/;
const DIMENSION = /^(\d+(?:\.\d+)?)(?:px|dp)?$/;

export function frontMatter(text: string): Dict {
  const match = /^---\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)/.exec(text);
  if (!match) throw new Error("DESIGN.md の先頭に YAML front matter（--- で囲む）がありません");
  const value = Bun.YAML.parse(match[1] as string);
  if (!isDict(value)) throw new Error("DESIGN.md の front matter はマッピングにしてください");
  return value;
}

function section(doc: Dict, name: string): Dict {
  const value = doc[name];
  if (!isDict(value) || Object.keys(value).length === 0) {
    throw new Error(`DESIGN.md の ${name} が空か、マッピングではありません`);
  }
  return value;
}

function dimensions(doc: Dict, name: string): [string, number][] {
  return Object.entries(section(doc, name))
    .map(([key, raw]): [string, number] => {
      const match = DIMENSION.exec(String(raw));
      if (!match) throw new Error(`${name}.${key}: "${String(raw)}" は 8 / 8px / 8dp の形で書いてください`);
      return [key, Number(match[1])];
    })
    .sort(([a], [b]) => compare(a, b));
}

const DARK_SUFFIX = "-dark";

function typography(doc: Dict): [string, number, number][] {
  return Object.entries(section(doc, "typography"))
    .map(([key, raw]): [string, number, number] => {
      if (!isDict(raw)) throw new Error(`typography.${key} はマッピングにしてください`);
      const size = DIMENSION.exec(String(raw.fontSize));
      if (!size)
        throw new Error(
          `typography.${key}.fontSize: "${String(raw.fontSize)}" は 15 / 15px / 15dp の形で書いてください`,
        );
      const weight = Number(raw.fontWeight);
      if (!Number.isInteger(weight) || weight < 100 || weight > 900 || weight % 100 !== 0) {
        throw new Error(
          `typography.${key}.fontWeight: "${String(raw.fontWeight)}" は 100〜900（100 刻み）で書いてください`,
        );
      }
      return [key, Number(size[1]), weight];
    })
    .sort(([a], [b]) => compare(a, b));
}

export function parse(text: string): Tokens {
  const doc = frontMatter(text);
  const all = section(doc, "colors");
  for (const [key, value] of Object.entries(all)) {
    if (!COLOR.test(String(value))) throw new Error(`colors.${key}: "${String(value)}" は #RRGGBB で書いてください`);
  }
  const names = Object.keys(all).filter((key) => !key.endsWith(DARK_SUFFIX));
  for (const key of Object.keys(all)) {
    if (key.endsWith(DARK_SUFFIX) && !names.includes(key.slice(0, -DARK_SUFFIX.length))) {
      throw new Error(`colors.${key} に対応する colors.${key.slice(0, -DARK_SUFFIX.length)} がありません`);
    }
  }
  const colors = names
    .map((key): [string, string, string] => {
      const dark = all[`${key}${DARK_SUFFIX}`];
      if (dark === undefined) throw new Error(`colors.${key} に対応する colors.${key}${DARK_SUFFIX} がありません`);
      return [key, String(all[key]), String(dark)];
    })
    .sort(([a], [b]) => compare(a, b));
  return {
    colors,
    rounded: dimensions(doc, "rounded"),
    spacing: dimensions(doc, "spacing"),
    typography: typography(doc),
  };
}
