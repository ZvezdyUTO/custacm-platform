// Author: huangbingrui.awa
import type { CollectionJobStartRequest, OjName } from '../types';

export function collectionRequest(usernames: string[], lookbackHours: number, ojName: OjName): CollectionJobStartRequest {
  return { usernames, lookbackHours, ojName, refreshWarehouse: true };
}
