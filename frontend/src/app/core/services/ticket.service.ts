import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ActiveTicket, TakenTicket, TicketDistance } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly ticketsUrl = `${environment.apiUrl}/api/tickets`;

  fetchMyActiveTicket(): Observable<ActiveTicket> {
    return this.http.get<ActiveTicket>(`${this.ticketsUrl}/me`);
  }

  takeTicket(serviceId: number, distance: TicketDistance): Observable<TakenTicket> {
    return this.http.post<TakenTicket>(this.ticketsUrl, { serviceId, distance });
  }

  cancelMyActiveTicket(): Observable<void> {
    return this.http.post<void>(`${this.ticketsUrl}/me/cancel`, null);
  }
}
