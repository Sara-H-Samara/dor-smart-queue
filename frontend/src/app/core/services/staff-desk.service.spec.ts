import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { CalledStudent, WaitingList } from '../models/staff-desk.model';
import { StaffDeskService } from './staff-desk.service';

const calledStudent: CalledStudent = {
  id: 14,
  ticketCode: 'R-014',
  studentName: 'Sample Student',
  status: 'CALLED',
  calledAt: '2026-11-16T09:30:00',
  counterName: 'Counter 2',
};

describe('StaffDeskService', () => {
  let staffDeskService: StaffDeskService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    staffDeskService = TestBed.inject(StaffDeskService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('fetchCurrentStudent_withCalledStudent_sendsGetAndReturnsStudent', () => {
    let received: CalledStudent | null = null;

    staffDeskService.fetchCurrentStudent(2).subscribe((student) => (received = student));

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/counters/2/current`);
    expect(request.request.method).toBe('GET');
    request.flush(calledStudent);
    expect(received).toEqual(calledStudent);
  });

  it('fetchCurrentStudent_whenServerAnswersNoContent_returnsNull', () => {
    let received: CalledStudent | null = calledStudent;

    staffDeskService.fetchCurrentStudent(2).subscribe((student) => (received = student));

    httpTesting
      .expectOne(`${environment.apiUrl}/api/counters/2/current`)
      .flush(null, { status: 204, statusText: 'No Content' });
    expect(received).toBeNull();
  });

  it('fetchWaitingList_withCounterId_sendsGetToWaitingPathAndReturnsList', () => {
    const waitingList: WaitingList = {
      waitingCount: 2,
      tickets: [
        { ticketCode: 'R-015', createdAt: '2026-11-16T09:31:00' },
        { ticketCode: 'R-016', createdAt: '2026-11-16T09:32:00' },
      ],
    };
    let received: WaitingList | null = null;

    staffDeskService.fetchWaitingList(2).subscribe((list) => (received = list));

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/counters/2/waiting`);
    expect(request.request.method).toBe('GET');
    request.flush(waitingList);
    expect(received).toEqual(waitingList);
  });

  it('callNextStudent_withCounterId_sendsPostToCallNextPath', () => {
    let received: CalledStudent | null = null;

    staffDeskService.callNextStudent(2).subscribe((student) => (received = student));

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/counters/2/call-next`);
    expect(request.request.method).toBe('POST');
    request.flush(calledStudent);
    expect(received).toEqual(calledStudent);
  });

  it('callNextStudent_whenNobodyIsWaiting_returnsNull', () => {
    let received: CalledStudent | null = calledStudent;

    staffDeskService.callNextStudent(2).subscribe((student) => (received = student));

    httpTesting
      .expectOne(`${environment.apiUrl}/api/counters/2/call-next`)
      .flush(null, { status: 204, statusText: 'No Content' });
    expect(received).toBeNull();
  });

  it('markTicketDone_withTicketId_sendsPostToDonePath', () => {
    staffDeskService.markTicketDone(14).subscribe();

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/tickets/14/done`);
    expect(request.request.method).toBe('POST');
    request.flush({ ...calledStudent, status: 'DONE' });
  });

  it('markTicketNoShow_withTicketId_sendsPostToNoShowPath', () => {
    staffDeskService.markTicketNoShow(14).subscribe();

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/tickets/14/no-show`);
    expect(request.request.method).toBe('POST');
    request.flush({ ...calledStudent, status: 'NO_SHOW' });
  });
});
