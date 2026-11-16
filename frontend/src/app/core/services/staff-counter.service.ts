import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AvailableCounter } from '../models/staff-desk.model';

@Injectable({ providedIn: 'root' })
export class StaffCounterService {
  private readonly http = inject(HttpClient);

  listAvailableCounters(): Observable<AvailableCounter[]> {
    return this.http.get<AvailableCounter[]>(`${environment.apiUrl}/api/counters`);
  }
}
