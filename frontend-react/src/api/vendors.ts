import client from './client';
import type { VendorConnection } from '../types';

export async function getConnections(): Promise<VendorConnection[]> {
  const { data } = await client.get<VendorConnection[]>('/api/vendors/connections');
  return data;
}

export async function connectToken(vendorType: string, accessToken: string): Promise<VendorConnection> {
  const { data } = await client.post<VendorConnection>('/api/vendors/connections/token', {
    vendorType,
    accessToken,
  });
  return data;
}

export async function connectLogin(vendorType: string, username: string, password: string): Promise<VendorConnection> {
  const { data } = await client.post<VendorConnection>('/api/vendors/connections/login', {
    vendorType,
    username,
    password,
  });
  return data;
}

export async function deleteConnection(id: number): Promise<void> {
  await client.delete(`/api/vendors/connections/${id}`);
}
