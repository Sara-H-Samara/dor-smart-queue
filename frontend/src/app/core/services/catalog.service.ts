import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CatalogItem, CatalogResource } from '../models/catalog.model';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  list(resource: CatalogResource): Observable<CatalogItem[]> {
    return this.http.get<CatalogItem[]>(this.url(resource));
  }

  create(resource: CatalogResource, name: string): Observable<CatalogItem> {
    return this.http.post<CatalogItem>(this.url(resource), { name });
  }

  rename(resource: CatalogResource, id: number, name: string): Observable<CatalogItem> {
    return this.http.put<CatalogItem>(`${this.url(resource)}/${id}`, { name });
  }

  setActive(resource: CatalogResource, id: number, active: boolean): Observable<CatalogItem> {
    return this.http.patch<CatalogItem>(`${this.url(resource)}/${id}/active`, { active });
  }

  private url(resource: CatalogResource): string {
    return `${environment.apiUrl}/api/admin/${resource}`;
  }
}
