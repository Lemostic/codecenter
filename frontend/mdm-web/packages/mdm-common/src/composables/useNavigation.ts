/**
 * common/composables/useNavigation.ts
 *
 * 多路径跳转公共方法（设计 §4.8）：
 * - 同一业务对象可能对应多个页面路径，由 useNavigation 统一维护
 * - 跳转前检查目标页面权限，无权限时给出提示或降级跳转
 * - 历史记录管理，确保前进/后退行为符合预期
 *
 * 用法：
 *   const { navigate } = useNavigation();
 *   navigate({ target: 'model-design-detail', params: { id } });
 *
 * ❌ 禁止：
 *   window.open(`#/model-design/${id}`);
 *   window.location.href = `...`;
 */
import { useRouter } from 'vue-router';
import { useAppStore } from '@mdm/common/stores/useAppStore';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';

export interface NavigateOptions {
  /** 目标路由 name 或 path */
  target: string;
  /** 路由 params */
  params?: Record<string, string>;
  /** 路由 query */
  query?: Record<string, string>;
  /** 是否新开标签页（默认 false） */
  newTab?: boolean;
  /**
   * 权限码（可选）。
   * 传了则跳转前校验；校验失败时弹出提示并中止跳转。
   * 校验逻辑基于 useAppStore().hasPermission。
   */
  permission?: string;
  /**
   * 多路径映射（可选）。
   * 同一业务对象对应多个页面路径时，传入映射表，
   * 函数内部按优先级匹配首个有权访问的路径。
   */
  multiPath?: Array<{ target: string; permission?: string }>;
}

export function useNavigation() {
  const router = useRouter();
  const appStore = useAppStore();

  /**
   * 内部：执行单次跳转
   */
  function doNavigate(target: string, options: NavigateOptions) {
    if (options.newTab) {
      const route = router.resolve({
        name: target,
        params: options.params,
        query: options.query,
      });
      window.open(route.href, '_blank', 'noopener,noreferrer');
      return;
    }
    router.push({
      name: target,
      params: options.params,
      query: options.query,
    });
  }

  /**
   * 内部：权限校验
   */
  function checkPermission(permission: string | undefined): boolean {
    if (!permission) return true;
    if (appStore.hasPermission(permission)) return true;
    TpMessage.warning('无访问权限');
    return false;
  }

  /**
   * 多路径降级：按顺序尝试首个有权访问的路径
   */
  function resolveMultiPath(multiPath: NavigateOptions['multiPath']): string | null {
    if (!multiPath || multiPath.length === 0) return null;
    for (const item of multiPath) {
      if (checkPermission(item.permission)) {
        return item.target;
      }
    }
    return null;
  }

  function navigate(options: NavigateOptions) {
    // 多路径模式：按优先级匹配首个有权路径
    if (options.multiPath && options.multiPath.length > 0) {
      const resolved = resolveMultiPath(options.multiPath);
      if (!resolved) {
        TpMessage.warning('暂无可访问的页面');
        return;
      }
      doNavigate(resolved, { ...options, target: resolved });
      return;
    }

    // 单路径模式：先校验权限
    if (!checkPermission(options.permission)) return;
    doNavigate(options.target, options);
  }

  return { navigate };
}
