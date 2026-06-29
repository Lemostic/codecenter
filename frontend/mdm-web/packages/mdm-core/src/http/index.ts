/**
 * core/http - 统一 HTTP 客户端
 *
 * 基于 Axios 封装，提供请求/响应拦截器。
 * 业务模块统一 import { http } from '@mdm/core/http' 使用。
 *
 * 注意：baseURL 强制为空（相对路径），所有 /api 请求由 Vite 代理转发到
 * 后端真实地址（配置在 vite.config.ts 的 server.proxy['/api'].target）。
 * 这样可以避免浏览器 CORS 问题。
 */
/// <reference types="vite/client" />
import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import type { ApiResponse } from '../types/api';
import { getToken, logout } from '../auth';

/** 创建 Axios 实例 */
const instance: AxiosInstance = axios.create({
  // 强制相对路径：通过 Vite 代理转发，避免跨域
  baseURL: '',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

/** 请求拦截器：Token 注入（从 @mdm/core/auth 读取） */
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

/** 响应拦截器：统一错误处理 */
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { data } = response;
    if (data.success === false) {
      // 业务错误：由拦截器统一提示，业务代码无需再判断
      console.error('[http]', data.message ?? '操作失败');
      return Promise.reject(new Error(data.message ?? '操作失败'));
    }
    return response;
  },
  (error) => {
    if (error.response) {
      const { status } = error.response;
      switch (status) {
        case 401:
          // Token 过期 / 未认证 → 调 logout()（清 token + 跳登录页）
          logout();
          break;
        case 403:
          console.error('[http] 无访问权限');
          break;
        case 500:
          console.error('[http] 服务器错误');
          break;
        default:
          console.error('[http]', error.message ?? '请求失败');
      }
    } else {
      console.error('[http] 网络连接失败');
    }
    return Promise.reject(error);
  },
);

/** 导出统一 http 实例 */
export const http = instance;
