import { TicketStatus } from './ticket.model';

export interface AvailableCounter {
  id: number;
  name: string;
  serviceId: number;
  serviceName: string;
}

export interface CalledStudent {
  id: number;
  ticketCode: string;
  studentName: string;
  status: TicketStatus;
  calledAt: string | null;
  counterName: string | null;
}
