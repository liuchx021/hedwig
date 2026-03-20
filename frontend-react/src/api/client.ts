import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const client = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** 需要自动登出的业务错误码（登录过期 / 令牌无效） */
const AUTO_LOGOUT_CODES = new Set([20000, 20003, 20004]);

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status: number | undefined = error.response?.status;
    const body = error.response?.data;

    const code = extractCode(body);
    const message = extractMessage(body) || fallbackMessage(status);

    // 仅在"登录态失效"类错误时自动登出，密码错误(20001)等不触发
    if (code !== undefined && AUTO_LOGOUT_CODES.has(code)) {
      useAuthStore.getState().logout();
      window.location.hash = '#/login';
    } else if (code === undefined && (status === 401 || status === 403)) {
      // 兼容：后端未返回 code 的 401/403（如 Spring Security 直接拦截）
      useAuthStore.getState().logout();
      window.location.hash = '#/login';
    }

    return Promise.reject(new ApiError(code ?? status ?? -1, message));
  },
);

// ==================== 工具函数 ====================

function extractCode(body: unknown): number | undefined {
  if (typeof body === 'object' && body !== null && 'code' in body) {
    const code = (body as { code: number }).code;
    return typeof code === 'number' ? code : undefined;
  }
  return undefined;
}

function extractMessage(body: unknown): string {
  if (typeof body === 'object' && body !== null && 'message' in body) {
    return String((body as { message: string }).message);
  }
  if (typeof body === 'string') return body.trim();
  return '';
}

function fallbackMessage(status: number | undefined): string {
  switch (status) {
    case 400: return '请求参数错误';
    case 401: return '登录状态已失效，请重新登录';
    case 403: return '没有权限执行此操作';
    case 404: return '请求的资源不存在';
    case 405: return '不支持的请求方式';
    case 429: return '请求过于频繁，请稍后再试';
    default:
      if (status && status >= 500) return '服务暂时不可用，请稍后重试';
      return `请求失败（HTTP ${status}）`;
  }
}

// ==================== 导出 ====================

/**
 * 业务错误，携带后端错误码。
 * 页面层可通过 `if (e instanceof ApiError)` 判断并读取 `e.code`。
 */
export class ApiError extends Error {
  readonly code: number;
  constructor(code: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
  }
}

export default client;
