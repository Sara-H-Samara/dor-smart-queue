export type TicketStatus = 'WAITING' | 'CALLED' | 'DONE' | 'NO_SHOW' | 'CANCELLED';

export type TicketDistance = 'NEAR' | 'MEDIUM' | 'FAR';

export interface ActiveTicket {
  ticketCode: string;
  serviceName: string;
  status: TicketStatus;
  position: number | null;
  peopleAhead: number | null;
  estimatedWaitMinutes: number | null;
  counterName: string | null;
  hasOpenCounter: boolean;
  distance: TicketDistance;
  turnIsNear: boolean;
  alertAtPeopleAhead: number | null;
}

export interface TakenTicket {
  id: number;
  ticketCode: string;
  serviceId: number;
  serviceName: string;
  status: TicketStatus;
  createdAt: string;
  distance: TicketDistance;
}
