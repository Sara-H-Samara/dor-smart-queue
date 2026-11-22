export interface CatalogItem {
  id: number;
  name: string;
  code?: string;
  active: boolean;
  serviceId?: number;
  serviceName?: string;
}

export type CatalogResource = 'services' | 'counters';
