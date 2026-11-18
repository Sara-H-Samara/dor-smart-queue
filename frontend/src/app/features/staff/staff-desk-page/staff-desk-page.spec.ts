import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject, of } from 'rxjs';

import {
  AvailableCounter,
  CalledStudent,
  WaitingList,
} from '../../../core/models/staff-desk.model';
import { AuthService } from '../../../core/services/auth.service';
import { ChosenCounterStorage } from '../../../core/services/chosen-counter-storage';
import { StaffCounterService } from '../../../core/services/staff-counter.service';
import { StaffDeskService } from '../../../core/services/staff-desk.service';
import { installDialogSupport } from '../../../testing/dialog-support';
import { failingRequestWithMessage, failingRequestWithStatus } from '../../../testing/http-failure';
import { StaffDeskPage } from './staff-desk-page';
import { WAITING_LIST_REFRESH_INTERVAL_MS } from './waiting-list-refresh';

const counterTwo: AvailableCounter = {
  id: 2,
  name: 'Counter 2',
  serviceId: 5,
  serviceName: 'Course registration',
};
const counterThree: AvailableCounter = {
  id: 3,
  name: 'Counter 3',
  serviceId: 6,
  serviceName: 'Document requests',
};
const calledStudent: CalledStudent = {
  id: 14,
  ticketCode: 'R-014',
  studentName: 'Sample Student',
  status: 'CALLED',
  calledAt: '2026-11-16T09:30:00',
  counterName: 'Counter 2',
};
const emptyWaitingList: WaitingList = { waitingCount: 0, tickets: [] };
const twoWaitingList: WaitingList = {
  waitingCount: 2,
  tickets: [
    { ticketCode: 'R-015', createdAt: '2026-11-16T09:31:00' },
    { ticketCode: 'R-016', createdAt: '2026-11-16T09:32:00' },
  ],
};
const oneWaitingList: WaitingList = {
  waitingCount: 1,
  tickets: [{ ticketCode: 'R-017', createdAt: '2026-11-16T09:33:00' }],
};

describe('StaffDeskPage', () => {
  let listAvailableCounters: ReturnType<typeof vi.fn>;
  let fetchCurrentStudent: ReturnType<typeof vi.fn>;
  let fetchWaitingList: ReturnType<typeof vi.fn>;
  let callNextStudent: ReturnType<typeof vi.fn>;
  let markTicketDone: ReturnType<typeof vi.fn>;
  let markTicketNoShow: ReturnType<typeof vi.fn>;
  let readChosenCounter: ReturnType<typeof vi.fn>;
  let saveChosenCounter: ReturnType<typeof vi.fn>;
  let clearChosenCounter: ReturnType<typeof vi.fn>;
  let logout: ReturnType<typeof vi.fn>;
  let navigateByUrl: ReturnType<typeof vi.fn>;

  beforeAll(() => installDialogSupport());

  beforeEach(() => {
    listAvailableCounters = vi.fn(() => of([counterTwo, counterThree]));
    fetchCurrentStudent = vi.fn(() => of(null));
    fetchWaitingList = vi.fn(() => of(emptyWaitingList));
    callNextStudent = vi.fn();
    markTicketDone = vi.fn();
    markTicketNoShow = vi.fn();
    readChosenCounter = vi.fn(() => 2);
    saveChosenCounter = vi.fn();
    clearChosenCounter = vi.fn();
    logout = vi.fn();
    navigateByUrl = vi.fn();

    TestBed.configureTestingModule({
      providers: [
        { provide: StaffCounterService, useValue: { listAvailableCounters } },
        {
          provide: StaffDeskService,
          useValue: {
            fetchCurrentStudent,
            fetchWaitingList,
            callNextStudent,
            markTicketDone,
            markTicketNoShow,
          },
        },
        {
          provide: ChosenCounterStorage,
          useValue: { read: readChosenCounter, save: saveChosenCounter, clear: clearChosenCounter },
        },
        { provide: AuthService, useValue: { logout } },
        { provide: Router, useValue: { navigateByUrl } },
      ],
    });
  });

  async function createPage(): Promise<ComponentFixture<StaffDeskPage>> {
    const fixture = TestBed.createComponent(StaffDeskPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  function pageElement(fixture: ComponentFixture<StaffDeskPage>): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function buttonWithText(
    fixture: ComponentFixture<StaffDeskPage>,
    text: string,
  ): HTMLButtonElement | undefined {
    return Array.from(pageElement(fixture).querySelectorAll('button')).find(
      (button) => button.textContent?.trim() === text,
    );
  }

  function textOf(fixture: ComponentFixture<StaffDeskPage>, selector: string): string | undefined {
    return pageElement(fixture).querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim();
  }

  function isCounterPickerOpen(fixture: ComponentFixture<StaffDeskPage>): boolean {
    return pageElement(fixture).querySelector('dialog')!.hasAttribute('open');
  }

  async function clickAndSettle(
    fixture: ComponentFixture<StaffDeskPage>,
    button: HTMLButtonElement | undefined,
  ): Promise<void> {
    button?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('opens the counter picker and disables Call next when no counter is stored', async () => {
    readChosenCounter.mockReturnValue(null);

    const fixture = await createPage();

    expect(isCounterPickerOpen(fixture)).toBe(true);
    expect(textOf(fixture, '.empty-state-title')).toBe('No counter chosen');
    expect(pageElement(fixture).querySelector<HTMLButtonElement>('.call-next-button')?.disabled).toBe(true);
    expect(fetchCurrentStudent).not.toHaveBeenCalled();
    expect(fetchWaitingList).not.toHaveBeenCalled();
    expect(pageElement(fixture).querySelector('.waiting-count')).toBeNull();
  });

  it('shows the counter and service in the header when the stored counter is listed', async () => {
    const fixture = await createPage();

    expect(textOf(fixture, '.page-header-context')).toBe('Counter 2 · Course registration');
    expect(fetchCurrentStudent).toHaveBeenCalledWith(2);
    expect(buttonWithText(fixture, 'Change counter')).toBeDefined();
  });

  it('clears the stored counter and asks to choose one when it is not in the list', async () => {
    readChosenCounter.mockReturnValue(99);

    const fixture = await createPage();

    expect(clearChosenCounter).toHaveBeenCalledTimes(1);
    expect(textOf(fixture, '.notice')).toBe('Your counter is no longer available. Choose another one.');
    expect(isCounterPickerOpen(fixture)).toBe(true);
    expect(pageElement(fixture).querySelector('.page-header-context')).toBeNull();
    expect(fetchCurrentStudent).not.toHaveBeenCalled();
  });

  it('shows the failure with Try again when the counter list fails', async () => {
    listAvailableCounters.mockReturnValue(failingRequestWithStatus(500));

    const fixture = await createPage();

    expect(textOf(fixture, '.alert')).toBe('Could not load the staff desk. Please try again.');
    expect(buttonWithText(fixture, 'Try again')).toBeDefined();
    expect(pageElement(fixture).querySelector('.call-next-button')).toBeNull();
  });

  it('shows the desk again when the staff member retries after a failed load', async () => {
    listAvailableCounters.mockReturnValueOnce(failingRequestWithStatus(500));
    const fixture = await createPage();

    await clickAndSettle(fixture, buttonWithText(fixture, 'Try again'));

    expect(pageElement(fixture).querySelector('.alert')).toBeNull();
    expect(textOf(fixture, '.page-header-context')).toBe('Counter 2 · Course registration');
  });

  it('shows the server message with Try again when the called student fails to load', async () => {
    fetchCurrentStudent.mockReturnValue(failingRequestWithMessage(404, 'Counter not found.'));

    const fixture = await createPage();

    expect(textOf(fixture, '.alert')).toBe('Counter not found.');
    expect(buttonWithText(fixture, 'Try again')).toBeDefined();
  });

  it('enables Served and No-show and disables Call next when a student is called', async () => {
    fetchCurrentStudent.mockReturnValue(of(calledStudent));

    const fixture = await createPage();

    expect(textOf(fixture, '.ticket-code')).toBe('R-014');
    expect(pageElement(fixture).querySelector<HTMLButtonElement>('.call-next-button')?.disabled).toBe(true);
    expect(buttonWithText(fixture, 'Served')?.disabled).toBe(false);
    expect(buttonWithText(fixture, 'No-show')?.disabled).toBe(false);
  });

  it('enables Call next when nobody is called', async () => {
    const fixture = await createPage();

    expect(pageElement(fixture).querySelector<HTMLButtonElement>('.call-next-button')?.disabled).toBe(false);
    expect(buttonWithText(fixture, 'Served')).toBeUndefined();
  });

  it('shows the idle line when nobody is called', async () => {
    const fixture = await createPage();

    expect(textOf(fixture, '.idle-ticket-message')).toBe('No student at your desk');
    expect(pageElement(fixture).textContent).not.toContain('Nobody is waiting');
  });

  it('shows the count line and a row for each waiting student', async () => {
    fetchWaitingList.mockReturnValue(of(twoWaitingList));

    const fixture = await createPage();

    expect(fetchWaitingList).toHaveBeenCalledWith(2);
    expect(textOf(fixture, '.waiting-count')).toBe('2 waiting for Course registration');
    expect(pageElement(fixture).querySelectorAll('.waiting-row')).toHaveLength(2);
    expect(textOf(fixture, '.waiting-row .list-row-name')).toBe('Course registration');
  });

  it('shows the called student, then Call next, then the waiting list', async () => {
    fetchWaitingList.mockReturnValue(of(twoWaitingList));

    const fixture = await createPage();

    const areaClasses = Array.from(pageElement(fixture).querySelector('.desk-columns')!.children).map(
      (areaElement) => areaElement.className,
    );
    expect(areaClasses).toEqual(['current-student-area', 'call-next-area', 'waiting-list-area']);
  });

  it('shows the choose-counter prompt before Call next when no counter is chosen', async () => {
    readChosenCounter.mockReturnValue(null);

    const fixture = await createPage();

    const areaClasses = Array.from(pageElement(fixture).querySelector('.desk-columns')!.children).map(
      (areaElement) => areaElement.className,
    );
    expect(areaClasses).toEqual(['choose-counter current-student-area', 'call-next-area']);
  });

  it('shows a zero count and keeps Call next enabled when the waiting list is empty', async () => {
    const fixture = await createPage();

    expect(textOf(fixture, '.waiting-count')).toBe('0 waiting for Course registration');
    expect(pageElement(fixture).querySelector('.waiting-heading')).toBeNull();
    expect(pageElement(fixture).querySelector<HTMLButtonElement>('.call-next-button')?.disabled).toBe(false);
  });

  it('shows no count line and no alert when the waiting list fails to load', async () => {
    fetchWaitingList.mockReturnValue(failingRequestWithStatus(500));

    const fixture = await createPage();

    expect(pageElement(fixture).querySelector('.waiting-count')).toBeNull();
    expect(pageElement(fixture).querySelector('.alert')).toBeNull();
    expect(pageElement(fixture).querySelector('.call-next-button')).not.toBeNull();
  });

  it('keeps the idle line and reloads the waiting list when nobody is waiting', async () => {
    callNextStudent.mockReturnValue(of(null));
    const fixture = await createPage();
    fetchWaitingList.mockClear();

    await clickAndSettle(fixture, buttonWithText(fixture, 'Call next'));

    expect(callNextStudent).toHaveBeenCalledWith(2);
    expect(fetchWaitingList).toHaveBeenCalledTimes(1);
    expect(textOf(fixture, '.idle-ticket-message')).toBe('No student at your desk');
    expect(pageElement(fixture).querySelector('.alert')).toBeNull();
    expect(pageElement(fixture).querySelector<HTMLButtonElement>('.call-next-button')?.disabled).toBe(false);
  });

  it('shows the reloaded waiting list after a student is called', async () => {
    fetchWaitingList.mockReturnValue(of(twoWaitingList));
    callNextStudent.mockReturnValue(of(calledStudent));
    const fixture = await createPage();
    fetchCurrentStudent.mockReturnValue(of(calledStudent));
    fetchWaitingList.mockReturnValue(of(oneWaitingList));

    await clickAndSettle(fixture, buttonWithText(fixture, 'Call next'));

    expect(textOf(fixture, '.waiting-count')).toBe('1 waiting for Course registration');
    expect(pageElement(fixture).querySelectorAll('.waiting-row')).toHaveLength(1);
  });

  it('shows the student card when a student is called', async () => {
    callNextStudent.mockReturnValue(of(calledStudent));
    const fixture = await createPage();
    fetchCurrentStudent.mockReturnValue(of(calledStudent));

    await clickAndSettle(fixture, buttonWithText(fixture, 'Call next'));

    expect(textOf(fixture, '.ticket-code')).toBe('R-014');
    expect(textOf(fixture, '.service-name')).toBe('Sample Student');
    expect(pageElement(fixture).querySelector<HTMLButtonElement>('.call-next-button')?.disabled).toBe(true);
  });

  it('shows the server message and reloads the desk when the server answers with a conflict', async () => {
    callNextStudent.mockReturnValue(
      failingRequestWithMessage(409, 'This counter is already serving a student.'),
    );
    const fixture = await createPage();
    fetchCurrentStudent.mockReturnValue(of(calledStudent));

    await clickAndSettle(fixture, buttonWithText(fixture, 'Call next'));

    expect(textOf(fixture, '.alert')).toBe('This counter is already serving a student.');
    expect(listAvailableCounters).toHaveBeenCalledTimes(2);
    expect(textOf(fixture, '.ticket-code')).toBe('R-014');
  });

  it('clears the chosen counter and asks to choose one when the counter is not found', async () => {
    callNextStudent.mockReturnValue(failingRequestWithMessage(404, 'Counter not found.'));
    const fixture = await createPage();
    listAvailableCounters.mockReturnValue(of([counterThree]));

    await clickAndSettle(fixture, buttonWithText(fixture, 'Call next'));

    expect(clearChosenCounter).toHaveBeenCalledTimes(1);
    expect(textOf(fixture, '.alert')).toBe('Counter not found.');
    expect(textOf(fixture, '.empty-state-title')).toBe('No counter chosen');
    expect(isCounterPickerOpen(fixture)).toBe(true);
  });

  it('shows the generic message and does not reload the desk when the server fails', async () => {
    callNextStudent.mockReturnValue(failingRequestWithStatus(500));
    const fixture = await createPage();

    await clickAndSettle(fixture, buttonWithText(fixture, 'Call next'));

    expect(textOf(fixture, '.alert')).toBe('The action could not be completed. Please try again.');
    expect(listAvailableCounters).toHaveBeenCalledTimes(1);
    expect(pageElement(fixture).querySelector<HTMLButtonElement>('.call-next-button')?.disabled).toBe(false);
  });

  it('shows the idle line after a student is marked done', async () => {
    fetchCurrentStudent.mockReturnValue(of(calledStudent));
    markTicketDone.mockReturnValue(of({ ...calledStudent, status: 'DONE' }));
    const fixture = await createPage();
    fetchCurrentStudent.mockReturnValue(of(null));

    await clickAndSettle(fixture, buttonWithText(fixture, 'Served'));

    expect(markTicketDone).toHaveBeenCalledWith(14);
    expect(textOf(fixture, '.idle-ticket-message')).toBe('No student at your desk');
    expect(pageElement(fixture).querySelector<HTMLButtonElement>('.call-next-button')?.disabled).toBe(false);
  });

  it('reloads the waiting list after a student is marked done', async () => {
    fetchCurrentStudent.mockReturnValue(of(calledStudent));
    markTicketDone.mockReturnValue(of({ ...calledStudent, status: 'DONE' }));
    fetchWaitingList.mockReturnValue(of(twoWaitingList));
    const fixture = await createPage();
    fetchCurrentStudent.mockReturnValue(of(null));
    fetchWaitingList.mockReturnValue(of(oneWaitingList));

    await clickAndSettle(fixture, buttonWithText(fixture, 'Served'));

    expect(fetchWaitingList).toHaveBeenCalledTimes(2);
    expect(textOf(fixture, '.waiting-count')).toBe('1 waiting for Course registration');
  });

  it('reloads the waiting list after a student is marked as a no-show', async () => {
    fetchCurrentStudent.mockReturnValue(of(calledStudent));
    markTicketNoShow.mockReturnValue(of({ ...calledStudent, status: 'NO_SHOW' }));
    fetchWaitingList.mockReturnValue(of(twoWaitingList));
    const fixture = await createPage();
    fetchCurrentStudent.mockReturnValue(of(null));
    fetchWaitingList.mockReturnValue(of(oneWaitingList));

    await clickAndSettle(fixture, buttonWithText(fixture, 'No-show'));

    expect(fetchWaitingList).toHaveBeenCalledTimes(2);
    expect(textOf(fixture, '.waiting-count')).toBe('1 waiting for Course registration');
  });

  it('sends the no-show for the ticket and reloads the called student', async () => {
    fetchCurrentStudent.mockReturnValue(of(calledStudent));
    markTicketNoShow.mockReturnValue(of({ ...calledStudent, status: 'NO_SHOW' }));
    const fixture = await createPage();
    fetchCurrentStudent.mockReturnValue(of(null));

    await clickAndSettle(fixture, buttonWithText(fixture, 'No-show'));

    expect(markTicketNoShow).toHaveBeenCalledWith(14);
    expect(fetchCurrentStudent).toHaveBeenCalledTimes(2);
    expect(buttonWithText(fixture, 'No-show')).toBeUndefined();
  });

  it('shows the server message and reloads the called student when a no-show is rejected', async () => {
    fetchCurrentStudent.mockReturnValue(of(calledStudent));
    markTicketNoShow.mockReturnValue(
      failingRequestWithMessage(409, 'Only a called ticket can be marked as no-show.'),
    );
    const fixture = await createPage();
    fetchCurrentStudent.mockReturnValue(of(null));

    await clickAndSettle(fixture, buttonWithText(fixture, 'No-show'));

    expect(textOf(fixture, '.alert')).toBe('Only a called ticket can be marked as no-show.');
    expect(fetchCurrentStudent).toHaveBeenCalledTimes(2);
    expect(textOf(fixture, '.idle-ticket-message')).toBe('No student at your desk');
  });

  it('disables all buttons while the done request runs', async () => {
    fetchCurrentStudent.mockReturnValue(of(calledStudent));
    markTicketDone.mockReturnValue(new Subject<CalledStudent>());
    const fixture = await createPage();

    await clickAndSettle(fixture, buttonWithText(fixture, 'Served'));

    expect(buttonWithText(fixture, 'Served')?.disabled).toBe(true);
    expect(buttonWithText(fixture, 'No-show')?.disabled).toBe(true);
    expect(pageElement(fixture).querySelector<HTMLButtonElement>('.call-next-button')?.disabled).toBe(true);
  });

  it('saves the counter id and loads the called student when a counter is chosen', async () => {
    readChosenCounter.mockReturnValue(null);
    const fixture = await createPage();
    fetchCurrentStudent.mockReturnValue(of(calledStudent));

    const counterButton = Array.from(
      pageElement(fixture).querySelectorAll<HTMLButtonElement>('.counter-option'),
    ).find((button) => button.textContent?.includes('Counter 3'));
    await clickAndSettle(fixture, counterButton);

    expect(saveChosenCounter).toHaveBeenCalledWith(3);
    expect(fetchCurrentStudent).toHaveBeenCalledWith(3);
    expect(textOf(fixture, '.page-header-context')).toBe('Counter 3 · Document requests');
    expect(pageElement(fixture).querySelector('.notice')).toBeNull();
  });

  it("drops the old waiting list and loads the new counter's list when the counter changes", async () => {
    fetchWaitingList.mockReturnValue(of(twoWaitingList));
    const fixture = await createPage();
    const waitingListOfNewCounter = new Subject<WaitingList>();
    fetchWaitingList.mockReturnValue(waitingListOfNewCounter);

    await clickAndSettle(fixture, buttonWithText(fixture, 'Change counter'));
    const counterButton = Array.from(
      pageElement(fixture).querySelectorAll<HTMLButtonElement>('.counter-option'),
    ).find((button) => button.textContent?.includes('Counter 3'));
    await clickAndSettle(fixture, counterButton);

    expect(fetchWaitingList).toHaveBeenLastCalledWith(3);
    expect(pageElement(fixture).querySelector('.waiting-count')).toBeNull();
    expect(pageElement(fixture).querySelector('.waiting-row')).toBeNull();

    waitingListOfNewCounter.next(oneWaitingList);
    fixture.detectChanges();

    expect(textOf(fixture, '.waiting-count')).toBe('1 waiting for Document requests');
  });

  it("ignores a late answer for the old counter's waiting list after the counter changes", async () => {
    const waitingListOfOldCounter = new Subject<WaitingList>();
    fetchWaitingList.mockReturnValue(waitingListOfOldCounter);
    const fixture = await createPage();
    fetchWaitingList.mockReturnValue(of(oneWaitingList));

    await clickAndSettle(fixture, buttonWithText(fixture, 'Change counter'));
    const counterButton = Array.from(
      pageElement(fixture).querySelectorAll<HTMLButtonElement>('.counter-option'),
    ).find((button) => button.textContent?.includes('Counter 3'));
    await clickAndSettle(fixture, counterButton);
    waitingListOfOldCounter.next(twoWaitingList);
    fixture.detectChanges();

    expect(textOf(fixture, '.waiting-count')).toBe('1 waiting for Document requests');
  });

  describe('polling the waiting list', () => {
    beforeEach(() => vi.useFakeTimers());

    afterEach(() => vi.useRealTimers());

    it('requests the list again and shows the new count after the refresh interval', async () => {
      fetchWaitingList.mockReturnValue(of(twoWaitingList));
      const fixture = await createPage();
      fetchWaitingList.mockReturnValue(of(oneWaitingList));

      vi.advanceTimersByTime(WAITING_LIST_REFRESH_INTERVAL_MS);
      fixture.detectChanges();

      expect(fetchWaitingList).toHaveBeenCalledTimes(2);
      expect(textOf(fixture, '.waiting-count')).toBe('1 waiting for Course registration');
    });

    it('sends no request before the refresh interval', async () => {
      await createPage();

      vi.advanceTimersByTime(WAITING_LIST_REFRESH_INTERVAL_MS - 1);

      expect(fetchWaitingList).toHaveBeenCalledTimes(1);
    });

    it('sends no second request while the previous one is still running', async () => {
      await createPage();
      fetchWaitingList.mockReturnValue(new Subject<WaitingList>());

      vi.advanceTimersByTime(WAITING_LIST_REFRESH_INTERVAL_MS);
      vi.advanceTimersByTime(WAITING_LIST_REFRESH_INTERVAL_MS);

      expect(fetchWaitingList).toHaveBeenCalledTimes(2);
    });

    it('keeps the last list and shows no alert when a request fails', async () => {
      fetchWaitingList.mockReturnValue(of(twoWaitingList));
      const fixture = await createPage();
      fetchWaitingList.mockReturnValue(failingRequestWithStatus(500));

      vi.advanceTimersByTime(WAITING_LIST_REFRESH_INTERVAL_MS);
      fixture.detectChanges();

      expect(fetchWaitingList).toHaveBeenCalledTimes(2);
      expect(textOf(fixture, '.waiting-count')).toBe('2 waiting for Course registration');
      expect(pageElement(fixture).querySelector('.alert')).toBeNull();
    });

    it('tries again at the next interval after a failed request', async () => {
      const fixture = await createPage();
      fetchWaitingList.mockReturnValueOnce(failingRequestWithStatus(500));
      vi.advanceTimersByTime(WAITING_LIST_REFRESH_INTERVAL_MS);
      fetchWaitingList.mockReturnValue(of(oneWaitingList));

      vi.advanceTimersByTime(WAITING_LIST_REFRESH_INTERVAL_MS);
      fixture.detectChanges();

      expect(fetchWaitingList).toHaveBeenCalledTimes(3);
      expect(textOf(fixture, '.waiting-count')).toBe('1 waiting for Course registration');
    });

    it('sends no request while a staff action is running', async () => {
      fetchCurrentStudent.mockReturnValue(of(calledStudent));
      markTicketDone.mockReturnValue(new Subject<CalledStudent>());
      const fixture = await createPage();
      await clickAndSettle(fixture, buttonWithText(fixture, 'Served'));

      vi.advanceTimersByTime(WAITING_LIST_REFRESH_INTERVAL_MS * 2);

      expect(fetchWaitingList).toHaveBeenCalledTimes(1);
    });

    it('sends no request when no counter is chosen', async () => {
      readChosenCounter.mockReturnValue(null);
      await createPage();

      vi.advanceTimersByTime(WAITING_LIST_REFRESH_INTERVAL_MS * 2);

      expect(fetchWaitingList).not.toHaveBeenCalled();
    });

    it('sends no request when the desk failed to load', async () => {
      listAvailableCounters.mockReturnValue(failingRequestWithStatus(500));
      await createPage();

      vi.advanceTimersByTime(WAITING_LIST_REFRESH_INTERVAL_MS * 2);

      expect(fetchWaitingList).not.toHaveBeenCalled();
    });
  });

  it('opens the picker and reloads the counter list when Change counter is clicked', async () => {
    const fixture = await createPage();
    listAvailableCounters.mockClear();

    await clickAndSettle(fixture, buttonWithText(fixture, 'Change counter'));

    expect(isCounterPickerOpen(fixture)).toBe(true);
    expect(listAvailableCounters).toHaveBeenCalledTimes(1);
  });

  it('shows the server message in the picker when the counter list fails', async () => {
    const fixture = await createPage();
    listAvailableCounters.mockReturnValue(failingRequestWithMessage(500, 'Something broke.'));

    await clickAndSettle(fixture, buttonWithText(fixture, 'Change counter'));

    expect(pageElement(fixture).querySelector('dialog .alert')?.textContent?.trim()).toBe('Something broke.');
  });

  it('clears the chosen counter and goes to the login page when Sign out is clicked', async () => {
    const fixture = await createPage();

    await clickAndSettle(fixture, buttonWithText(fixture, 'Sign out'));

    expect(clearChosenCounter).toHaveBeenCalledTimes(1);
    expect(logout).toHaveBeenCalledTimes(1);
    expect(navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
