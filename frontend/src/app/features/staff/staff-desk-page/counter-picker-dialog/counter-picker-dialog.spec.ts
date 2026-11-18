import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AvailableCounter } from '../../../../core/models/staff-desk.model';
import { installDialogSupport } from '../../../../testing/dialog-support';
import { CounterPickerDialog } from './counter-picker-dialog';

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

describe('CounterPickerDialog', () => {
  let fixture: ComponentFixture<CounterPickerDialog>;
  let pickerElement: HTMLElement;

  beforeAll(() => installDialogSupport());

  beforeEach(() => {
    fixture = TestBed.createComponent(CounterPickerDialog);
    pickerElement = fixture.nativeElement as HTMLElement;
  });

  function counterButtons(): HTMLButtonElement[] {
    return Array.from(pickerElement.querySelectorAll<HTMLButtonElement>('.counter-option'));
  }

  function cancelButton(): HTMLButtonElement {
    return pickerElement.querySelector<HTMLButtonElement>('.dialog-actions button')!;
  }

  function isDialogOpen(): boolean {
    return pickerElement.querySelector('dialog')!.hasAttribute('open');
  }

  it('opens the dialog when open is called', () => {
    fixture.detectChanges();

    fixture.componentInstance.open();

    expect(isDialogOpen()).toBe(true);
  });

  it('lists the name and service name of each counter', () => {
    fixture.componentRef.setInput('counters', [counterTwo, counterThree]);

    fixture.detectChanges();

    const namesAndServices = counterButtons().map((button) => [
      button.querySelector('.list-row-name')?.textContent?.trim(),
      button.querySelector('.list-row-detail')?.textContent?.trim(),
    ]);
    expect(namesAndServices).toEqual([
      ['Counter 2', 'Course registration'],
      ['Counter 3', 'Document requests'],
    ]);
  });

  it('marks only the chosen counter as current', () => {
    fixture.componentRef.setInput('counters', [counterTwo, counterThree]);
    fixture.componentRef.setInput('chosenCounterId', 3);

    fixture.detectChanges();

    const [buttonForCounterTwo, buttonForCounterThree] = counterButtons();
    expect(buttonForCounterTwo.getAttribute('aria-current')).toBeNull();
    expect(buttonForCounterThree.getAttribute('aria-current')).toBe('true');
    expect(pickerElement.querySelectorAll('.counter-current-label').length).toBe(1);
  });

  it('shows the empty state when there are no counters', () => {
    fixture.componentRef.setInput('counters', []);

    fixture.detectChanges();

    expect(pickerElement.querySelector('.empty-state-title')?.textContent?.trim()).toBe(
      'No counters available',
    );
    expect(counterButtons().length).toBe(0);
  });

  it('shows loading and no counter list while loading', () => {
    fixture.componentRef.setInput('counters', [counterTwo]);
    fixture.componentRef.setInput('isLoading', true);

    fixture.detectChanges();

    expect(pickerElement.querySelector('.notice')?.textContent?.trim()).toBe('Loading…');
    expect(counterButtons().length).toBe(0);
  });

  it('shows the failure alert without the empty state', () => {
    fixture.componentRef.setInput('failureMessage', 'Could not reach the server.');

    fixture.detectChanges();

    expect(pickerElement.querySelector('.alert')?.textContent?.trim()).toBe(
      'Could not reach the server.',
    );
    expect(pickerElement.querySelector('.empty-state')).toBeNull();
  });

  it('emits the counter id and closes the dialog without emitting closed', () => {
    const counterChosen = vi.fn();
    const closed = vi.fn();
    fixture.componentInstance.counterChosen.subscribe(counterChosen);
    fixture.componentInstance.closed.subscribe(closed);
    fixture.componentRef.setInput('counters', [counterTwo, counterThree]);
    fixture.detectChanges();
    fixture.componentInstance.open();

    counterButtons()[1].click();

    expect(counterChosen).toHaveBeenCalledWith(3);
    expect(closed).not.toHaveBeenCalled();
    expect(isDialogOpen()).toBe(false);
  });

  it('emits closed when dismissed after an earlier choice', () => {
    const closed = vi.fn();
    fixture.componentInstance.closed.subscribe(closed);
    fixture.componentRef.setInput('counters', [counterTwo]);
    fixture.detectChanges();
    fixture.componentInstance.open();
    counterButtons()[0].click();
    fixture.componentInstance.open();

    pickerElement.querySelector('dialog')!.dispatchEvent(new Event('close'));

    expect(closed).toHaveBeenCalledTimes(1);
  });

  it('emits closed when the dialog is closed without a choice', () => {
    const closed = vi.fn();
    fixture.componentInstance.closed.subscribe(closed);
    fixture.detectChanges();
    fixture.componentInstance.open();

    pickerElement.querySelector('dialog')!.dispatchEvent(new Event('close'));

    expect(closed).toHaveBeenCalledTimes(1);
  });

  it('closes the dialog without choosing a counter when cancel is clicked', () => {
    const counterChosen = vi.fn();
    const closed = vi.fn();
    fixture.componentInstance.counterChosen.subscribe(counterChosen);
    fixture.componentInstance.closed.subscribe(closed);
    fixture.componentRef.setInput('counters', [counterTwo]);
    fixture.detectChanges();
    fixture.componentInstance.open();

    cancelButton().click();

    expect(isDialogOpen()).toBe(false);
    expect(closed).toHaveBeenCalledTimes(1);
    expect(counterChosen).not.toHaveBeenCalled();
  });
});
