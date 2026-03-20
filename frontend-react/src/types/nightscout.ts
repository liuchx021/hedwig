// Nightscout 推送目标类型定义

export interface NightscoutTarget {
  id: number;
  name: string;
  baseUrl: string;
  apiSecretHint?: string;
  status: 'ACTIVE' | 'DISABLED';
  isDefault: boolean;
  monitoredSubjectId?: number;
  monitoredSubjectName?: string;
  lastPushAt?: string;
  lastSuccessAt?: string;
  lastErrorMessage?: string;
  createdAt?: string;
}

export interface NightscoutTargetRequest {
  name: string;
  baseUrl: string;
  apiSecret?: string;
  monitoredSubjectId?: number;
  isDefault?: boolean;
}

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}
