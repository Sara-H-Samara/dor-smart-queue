export interface CounterOnScreen {
  counterName: string;
  serviceName: string;
  ticketCode: string | null;
}

export interface NextTicketOnScreen {
  ticketCode: string;
  serviceName: string;
}

export interface WaitingRoom {
  counters: CounterOnScreen[];
  nextTickets: NextTicketOnScreen[];
  moreWaitingCount: number;
}
