import { TicketDistance } from '../../../../core/models/ticket.model';

export interface DistanceOption {
  value: TicketDistance;
  name: string;
  description: string;
}

export const DISTANCE_OPTIONS: readonly DistanceOption[] = [
  { value: 'NEAR', name: 'Near', description: 'I am close to the counters' },
  { value: 'MEDIUM', name: 'Medium', description: 'A few minutes away' },
  { value: 'FAR', name: 'Far', description: '10 minutes or more away' },
];
