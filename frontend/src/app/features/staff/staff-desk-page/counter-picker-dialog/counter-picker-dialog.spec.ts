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

  it('open_whenCalled_opensTheDialog', () => {
    fixture.detectChanges();

    fixture.componentInstance.open();

    expect(isDialogOpen()).toBe(true);
  });

  it('render_withCounters_listsNameAndServiceNameOfEachCounter', () => {
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

  it('render_withChosenCounter_marksOnlyThatCounterAsCurrent', () => {
    fixture.componentRef.setInput('counters', [counterTwo, counterThree]);
    fixture.componentRef.setInput('chosenCounterId', 3);

    fixture.detectChanges();

    const [buttonForCounterTwo, buttonForCounterThree] = counterButtons();
    expect(buttonForCounterTwo.getAttribute('aria-current')).toBeNull();
    expect(buttonForCounterThree.getAttribute('aria-current')).toBe('true');
    expect(pickerElement.querySelectorAll('.counter-current-label').length).toBe(1);
  });

  it('render_withNoCounters_showsEmptyState', () => {
    fixture.componentRef.setInput('counters', []);

    fixture.detectChanges();

    expect(pickerElement.querySelector('.empty-state-title')?.textContent?.trim()).toBe(
      'No counters available',
    );
    expect(counterButtons().length).toBe(0);
  });

  it('render_whileLoading_showsLoadingAndNoCounterList', () => {
    fixture.componentRef.setInput('counters', [counterTwo]);
    fixture.componentRef.setInput('isLoading', true);

    fixture.detectChanges();

    expect(pickerElement.querySelector('.notice')?.textContent?.trim()).toBe('Loading…');
    expect(counterButtons().length).toBe(0);
  });

  it('render_withFailureMessage_showsAlertWithoutEmptyState', () => {
    fixture.componentRef.setInput('failureMessage', 'Could not reach the server.');

    fixture.detectChanges();

    expect(pickerElement.querySelector('.alert')?.textContent?.trim()).toBe(
      'Could not reach the server.',
    );
    expect(pickerElement.querySelector('.empty-state')).toBeNull();
  });

  it('click_onCounter_emitsCounterIdAndClosesTheDialog', () => {
    const counterChosen = vi.fn();
    fixture.componentInstance.counterChosen.subscribe(counterChosen);
    fixture.componentRef.setInput('counters', [counterTwo, counterThree]);
    fixture.detectChanges();
    fixture.componentInstance.open();

    counterButtons()[1].click();

    expect(counterChosen).toHaveBeenCalledWith(3);
    expect(isDialogOpen()).toBe(false);
  });

  it('click_onCancel_closesTheDialogWithoutChoosingACounter', () => {
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
