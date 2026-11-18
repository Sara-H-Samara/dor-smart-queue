import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { WaitingRoom } from '../models/waiting-room.model';
import { WaitingRoomService } from './waiting-room.service';

describe('WaitingRoomService', () => {
  let waitingRoomService: WaitingRoomService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    waitingRoomService = TestBed.inject(WaitingRoomService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('sends one GET request to the public queue status path and returns the waiting room', () => {
    const waitingRoom: WaitingRoom = {
      counters: [{ counterName: 'Counter 1', serviceName: 'Registration', ticketCode: 'R-007' }],
      nextTickets: [{ ticketCode: 'R-008', serviceName: 'Registration' }],
      moreWaitingCount: 2,
    };
    let receivedWaitingRoom: WaitingRoom | null = null;

    waitingRoomService.fetchWaitingRoom().subscribe((room) => (receivedWaitingRoom = room));

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/public/queue-status`);
    expect(request.request.method).toBe('GET');
    request.flush(waitingRoom);
    expect(receivedWaitingRoom).toEqual(waitingRoom);
  });
});
