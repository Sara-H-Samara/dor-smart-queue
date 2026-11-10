export interface CatalogItem {
  id: number;
  name: string;
  /** Present on services only; counters have no code. */
  code?: string;
  active: boolean;
}

export type CatalogResource = 'services' | 'counters';
