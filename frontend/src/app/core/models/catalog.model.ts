export interface CatalogItem {
  id: number;
  name: string;
  active: boolean;
}

export type CatalogResource = 'services' | 'counters';
