import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CalledStudent, WaitingList } from '../models/staff-desk.model';

@Injectable({ providedIn: 'root' })
export class StaffDeskService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api`;

  fetchCurrentStudent(counterId: number): Observable<CalledStudent | null> {
    return this.http.get<CalledStudent | null>(`${this.apiUrl}/counters/${counterId}/current`);
  }

  fetchWaitingList(counterId: number): Observable<WaitingList> {
    return this.http.get<WaitingList>(`${this.apiUrl}/counters/${counterId}/waiting`);
  }

  callNextStudent(counterId: number): Observable<CalledStudent | null> {
    return this.http.post<CalledStudent | null>(`${this.apiUrl}/counters/${counterId}/call-next`, null);
  }

  markTicketDone(ticketId: number): Observable<CalledStudent> {
    return this.http.post<CalledStudent>(`${this.apiUrl}/tickets/${ticketId}/done`, null);
  }

  markTicketNoShow(ticketId: number): Observable<CalledStudent> {
    return this.http.post<CalledStudent>(`${this.apiUrl}/tickets/${ticketId}/no-show`, null);
  }
}
