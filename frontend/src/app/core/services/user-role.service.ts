import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { UserPage, UserRole, UserRow } from '../models/user-role.model';

@Injectable({ providedIn: 'root' })
export class UserRoleService {
  private readonly http = inject(HttpClient);
  private readonly usersUrl = `${environment.apiUrl}/api/admin/users`;

  listUsers(search: string, page: number, size: number): Observable<UserPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    const trimmedSearch = search.trim();
    if (trimmedSearch !== '') {
      params = params.set('search', trimmedSearch);
    }
    return this.http.get<UserPage>(this.usersUrl, { params });
  }

  changeRole(id: number, role: UserRole): Observable<UserRow> {
    return this.http.patch<UserRow>(`${this.usersUrl}/${id}/role`, { role });
  }
}
