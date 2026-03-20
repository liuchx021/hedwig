import client from './client';
import type { GlucoseReading, SyncResult } from '../types';

export async function getHistory(subjectId: string): Promise<GlucoseReading[]> {
  const { data } = await client.get<GlucoseReading[]>(`/api/glucose/subjects/${subjectId}/readings`);
  return data;
}

export async function syncHistory(subjectId: string): Promise<SyncResult> {
  const { data } = await client.post<SyncResult>(`/api/glucose/subjects/${subjectId}/sync`);
  return data;
}

export async function getLatest(subjectId: string | number): Promise<GlucoseReading | null> {
  try {
    const { data } = await client.get<GlucoseReading>(`/api/glucose/subjects/${subjectId}/readings/latest`);
    return data;
  } catch {
    return null;
  }
}
