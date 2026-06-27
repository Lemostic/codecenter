/**
 * 配置加载入口。
 *
 * 流程：
 *   1. provider.load() 获取部分配置
 *   2. 与 DEFAULT_CONFIG 深度合并
 *   3. applyConfig() 写入运行时（CSS 变量 / 字号）
 *   4. 失败时静默降级为 DEFAULT_CONFIG
 */
import type { ConfigProvider, PlatformConfig } from './types';
import { DEFAULT_CONFIG } from './defaults';
import { applyConfig } from './apply';
import { deepMerge } from './utils';

export async function loadAndApplyConfig(provider: ConfigProvider): Promise<PlatformConfig> {
  let config: PlatformConfig = { ...DEFAULT_CONFIG };

  try {
    const external = await provider.load();
    config = deepMerge(
      DEFAULT_CONFIG as unknown as Record<string, unknown>,
      external as unknown as Record<string, unknown>,
    ) as unknown as PlatformConfig;
  } catch (err) {
    // 配置获取失败 → 静默降级为 DEFAULT_CONFIG
    console.warn('[config] 配置获取失败，使用框架固定值', err);
  }

  applyConfig(config);
  return config;
}
