/**
 * 配置工具函数：颜色混合、深合并、Element Plus 色阶派生。
 *
 * 不引入 lodash 等第三方库，全部手写实现。
 */

/** 将 hex 颜色与白色/黑色按百分比混合（hex → rgb → mix → hex） */
export function mixColor(color1: string, color2: string, weight: number): string {
  // weight: color1 在结果中的占比（0~1）
  const d = (hex: string) => parseInt(hex.replace('#', ''), 16);
  const c1 = d(color1);
  const c2 = d(color2);
  const r = Math.round(((c1 >> 16) & 0xff) * weight + ((c2 >> 16) & 0xff) * (1 - weight));
  const g = Math.round(((c1 >> 8) & 0xff) * weight + ((c2 >> 8) & 0xff) * (1 - weight));
  const b = Math.round((c1 & 0xff) * weight + ((c2 & 0xff) * (1 - weight)));
  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`;
}

/**
 * 根据主色生成 Element Plus 所需的色阶变量，
 * 写入 :root 的 CSS 变量：
 *   --el-color-primary-light-3 / -5 / -7 / -8 / -9
 *   --el-color-primary-dark-2
 */
export function generateColorVariants(primaryColor: string): void {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  const lightSteps = [3, 5, 7, 8, 9];
  for (const step of lightSteps) {
    root.style.setProperty(
      `--el-color-primary-light-${step}`,
      mixColor(primaryColor, '#ffffff', 1 - step / 10),
    );
  }
  root.style.setProperty(
    '--el-color-primary-dark-2',
    mixColor(primaryColor, '#000000', 0.8),
  );
}

/**
 * 深度合并：以后者字段覆盖前者字段。
 * 仅做"字段覆盖"——不做数组合并、不做对象深引用。
 * 数组 / Date / RegExp 等引用类型按"后值覆盖"处理。
 */
export function deepMerge<T extends Record<string, unknown>>(
  base: T,
  override: Partial<T> | undefined,
): T {
  if (!override) return base;
  const result: Record<string, unknown> = { ...base };
  for (const key of Object.keys(override) as (keyof T)[]) {
    const baseVal = base[key];
    const overrideVal = override[key];
    if (
      baseVal && overrideVal
      && typeof baseVal === 'object' && !Array.isArray(baseVal)
      && typeof overrideVal === 'object' && !Array.isArray(overrideVal)
    ) {
      result[key as string] = deepMerge(
        baseVal as Record<string, unknown>,
        overrideVal as Record<string, unknown>,
      );
    } else {
      result[key as string] = overrideVal;
    }
  }
  return result as T;
}
