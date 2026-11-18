import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { WaitingRoom } from '../models/waiting-room.model';

export const WAITING_ROOM_PATH = '/api/public/queue-status';

@Injectable({ providedIn: 'root' })
export class WaitingRoomService {
  private readonly http = inject(HttpClient);

  fetchWaitingRoom(): Observable<WaitingRoom> {
    return this.http.get<WaitingRoom>(`${environment.apiUrl}${WAITING_ROOM_PATH}`);
  }
}
