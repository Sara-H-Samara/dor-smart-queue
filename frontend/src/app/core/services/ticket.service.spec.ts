import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { TicketService } from './ticket.service';

describe('TicketService', () => {
  let ticketService: TicketService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    ticketService = TestBed.inject(TicketService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('sends the chosen distance together with the service id when taking a ticket', () => {
    ticketService.takeTicket(3, 'FAR').subscribe();

    const request = httpTesting.expectOne(`${environment.apiUrl}/api/tickets`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ serviceId: 3, distance: 'FAR' });
  });
});
