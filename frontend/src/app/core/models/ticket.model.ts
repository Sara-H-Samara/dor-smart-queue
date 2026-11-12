export type TicketStatus = 'WAITING' | 'CALLED' | 'SERVED' | 'NO_SHOW' | 'CANCELLED';

export interface ActiveTicket {
  ticketCode: string;
  serviceName: string;
  status: TicketStatus;
  position: number | null;
  peopleAhead: number | null;
  estimatedWaitMinutes: number | null;
}

export interface TakenTicket {
  id: number;
  ticketCode: string;
  serviceId: number;
  serviceName: string;
  status: TicketStatus;
  createdAt: string;
}
