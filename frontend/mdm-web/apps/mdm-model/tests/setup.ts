/**
 * 全局测试 setup — 所有 spec 文件共享
 * 通过 vitest.config.ts 的 setupFiles 引入
 */
import { vi } from 'vitest';

// ========== 全局 Mock UI 反馈组件 ==========
vi.mock('@mdm/common/components/feedback/TpMessage', () => ({
  TpMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}));

vi.mock('@mdm/common/components/feedback/TpConfirm', () => ({
  TpConfirm: {
    delete: vi.fn(),
    confirm: vi.fn(),
  },
}));
