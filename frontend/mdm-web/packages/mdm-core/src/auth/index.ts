/**
 * core/auth - Token 管理与权限校验
 *
 * 提供 token 顶层函数：getToken / setToken / removeToken / isLoggedIn / logout。
 * token 存储于 localStorage，key 固定为 'mdm_token'。
 *
 * 不存业务用户信息——业务信息放在 common/stores 或模块 store 中。
 */

const TOKEN_KEY = 'mdm_token';

/** 获取 token */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

/** 设置 token */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

/** 移除 token */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

/** 是否已登录 */
export function isLoggedIn(): boolean {
  return !!getToken();
}

/** 登出：清 token + 跳转登录页 */
export function logout(): void {
  removeToken();
  // 登录页跳转由主系统处理；此处仅清 token，
  // http 拦截器发现 401 会触发业务侧统一跳转逻辑
  if (typeof window !== 'undefined') {
    window.location.hash = '#/login';
  }
}
