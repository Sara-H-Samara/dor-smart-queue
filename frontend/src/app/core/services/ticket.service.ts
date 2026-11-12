import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ActiveTicket, TakenTicket } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly ticketsUrl = `${environment.apiUrl}/api/tickets`;

  fetchMyActiveTicket(): Observable<ActiveTicket> {
    return this.http.get<ActiveTicket>(`${this.ticketsUrl}/me`);
  }

  takeTicket(serviceId: number): Observable<TakenTicket> {
    return this.http.post<TakenTicket>(this.ticketsUrl, { serviceId });
  }

  cancelMyActiveTicket(): Observable<void> {
    return this.http.post<void>(`${this.ticketsUrl}/me/cancel`, null);
  }
}
