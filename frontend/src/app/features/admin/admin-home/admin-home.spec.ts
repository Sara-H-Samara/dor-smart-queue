import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { environment } from '../../../../environments/environment';
import { LiveUpdateService } from '../../../core/live-updates/live-update.service';
import { AuthService } from '../../../core/services/auth.service';
import { installDialogSupport } from '../../../testing/dialog-support';
import { FakeLiveUpdateService } from '../../../testing/fake-live-update-service';
import { AdminHome } from './admin-home';

const servicesUrl = `${environment.apiUrl}/api/admin/services`;
const countersUrl = `${environment.apiUrl}/api/admin/counters`;

describe('AdminHome', () => {
  let fixture: ComponentFixture<AdminHome>;
  let httpTesting: HttpTestingController;
  let fakeLiveUpdateService: FakeLiveUpdateService;

  beforeAll(() => installDialogSupport());

  beforeEach(() => {
    fakeLiveUpdateService = new FakeLiveUpdateService();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LiveUpdateService, useValue: fakeLiveUpdateService },
        { provide: AuthService, useValue: {} },
        { provide: Router, useValue: {} },
      ],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminHome);
    fixture.detectChanges();
    httpTesting.expectNone(servicesUrl);
    httpTesting.expectNone(countersUrl);
    fakeLiveUpdateService.liveUpdateWatch.requestFirstLoad();
    httpTesting.expectOne(servicesUrl).flush([]);
    httpTesting.expectOne(countersUrl).flush([]);
    fixture.detectChanges();
  });

  afterEach(() => httpTesting.verify());

  it('watches the admin stream once for both panels', () => {
    expect(fakeLiveUpdateService.watchedLiveStreams).toEqual(['admin']);
  });

  it('asks both panels to load their lists again when a refresh is requested', () => {
    fakeLiveUpdateService.liveUpdateWatch.requestRefresh();

    httpTesting.expectOne(servicesUrl).flush([]);
    httpTesting.expectOne(countersUrl).flush([]);
  });

  it('firstLoadRequest_afterTheFirstLoad_sendsNoSecondRequest', () => {
    httpTesting.expectNone(servicesUrl);
    httpTesting.expectNone(countersUrl);
  });

  it('sends no request when no refresh is requested', () => {
    httpTesting.expectNone(servicesUrl);
    httpTesting.expectNone(countersUrl);
  });
});
