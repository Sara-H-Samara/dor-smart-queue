import { TicketStatus } from './ticket.model';

export interface AvailableCounter {
  id: number;
  name: string;
  serviceId: number;
  serviceName: string;
}

export interface WaitingTicket {
  ticketCode: string;
  createdAt: string;
}

export interface WaitingList {
  waitingCount: number;
  tickets: WaitingTicket[];
}

export interface CalledStudent {
  id: number;
  ticketCode: string;
  studentName: string;
  status: TicketStatus;
  calledAt: string | null;
  counterName: string | null;
}
