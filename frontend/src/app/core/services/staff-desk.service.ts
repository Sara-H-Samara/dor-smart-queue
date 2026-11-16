import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CalledStudent } from '../models/staff-desk.model';

/** The server answers 204 when there is no student, which arrives here as null. */
@Injectable({ providedIn: 'root' })
export class StaffDeskService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api`;

  fetchCurrentStudent(counterId: number): Observable<CalledStudent | null> {
    return this.http.get<CalledStudent | null>(`${this.apiUrl}/counters/${counterId}/current`);
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
