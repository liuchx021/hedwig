import client from './client';
import type { NightscoutTarget, NightscoutTargetRequest, ApiResult } from '../types/nightscout';

export async function listTargets(): Promise<NightscoutTarget[]> {
  const { data } = await client.get<ApiResult<NightscoutTarget[]>>('/api/nightscout/targets');
  return data.data;
}

export async function getTarget(id: number): Promise<NightscoutTarget> {
  const { data } = await client.get<ApiResult<NightscoutTarget>>(`/api/nightscout/targets/${id}`);
  return data.data;
}

export async function createTarget(request: NightscoutTargetRequest): Promise<NightscoutTarget> {
  const { data } = await client.post<ApiResult<NightscoutTarget>>('/api/nightscout/targets', request);
  return data.data;
}

export async function updateTarget(id: number, request: Partial<NightscoutTargetRequest>): Promise<NightscoutTarget> {
  const { data } = await client.put<ApiResult<NightscoutTarget>>(`/api/nightscout/targets/${id}`, request);
  return data.data;
}

export async function deleteTarget(id: number): Promise<void> {
  await client.delete(`/api/nightscout/targets/${id}`);
}

export async function toggleTargetStatus(id: number): Promise<NightscoutTarget> {
  const { data } = await client.post<ApiResult<NightscoutTarget>>(`/api/nightscout/targets/${id}/toggle`);
  return data.data;
}

export async function nightscoutSync(): Promise<{ count: number; message: string }> {
  const { data } = await client.post<ApiResult<number>>('/api/nightscout/sync');
  return { count: data.data, message: data.message };
}
