import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ActiveService } from '../models/active-service.model';

@Injectable({ providedIn: 'root' })
export class StudentCatalogService {
  private readonly http = inject(HttpClient);

  listActiveServices(): Observable<ActiveService[]> {
    return this.http.get<ActiveService[]>(`${environment.apiUrl}/api/services`);
  }
}
