/**
 * StaticProvider — 返回框架内置固定值。
 *
 * 当前阶段使用。后续确定配置来源后（API / 文件 / SDK），
 * 新增对应 Provider 实现并在此切换即可，应用层代码不变。
 */
import type { ConfigProvider, PlatformConfig } from '../types';
import { DEFAULT_CONFIG } from '../defaults';

export class StaticProvider implements ConfigProvider {
  async load(): Promise<PlatformConfig> {
    return { ...DEFAULT_CONFIG };
  }
}
