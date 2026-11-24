import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TicketDistance } from '../../../../core/models/ticket.model';
import { DistancePicker } from './distance-picker';

describe('DistancePicker', () => {
  let fixture: ComponentFixture<DistancePicker>;
  let chosenDistances: TicketDistance[];

  function render(selectedDistance: TicketDistance, isDisabled = false): void {
    fixture = TestBed.createComponent(DistancePicker);
    chosenDistances = [];
    fixture.componentInstance.distanceChosen.subscribe((distance) =>
      chosenDistances.push(distance),
    );
    fixture.componentRef.setInput('selectedDistance', selectedDistance);
    fixture.componentRef.setInput('isDisabled', isDisabled);
    fixture.detectChanges();
  }

  function radioInputs(): HTMLInputElement[] {
    return Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('input[type="radio"]'),
    );
  }

  function radioInputFor(distance: TicketDistance): HTMLInputElement {
    return radioInputs().find((input) => input.value === distance) as HTMLInputElement;
  }

  it('shows the near, medium and far options with their descriptions', () => {
    render('NEAR');

    const pickerText = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(radioInputs().map((input) => input.value)).toEqual(['NEAR', 'MEDIUM', 'FAR']);
    expect(pickerText).toContain('I am close to the counters');
    expect(pickerText).toContain('A few minutes away');
    expect(pickerText).toContain('10 minutes or more away');
  });

  it('marks only the selected distance as checked', () => {
    render('FAR');

    expect(radioInputFor('FAR').checked).toBe(true);
    expect(radioInputFor('NEAR').checked).toBe(false);
    expect(radioInputFor('MEDIUM').checked).toBe(false);
  });

  it('emits the distance when an option is chosen', () => {
    render('NEAR');

    radioInputFor('MEDIUM').click();

    expect(chosenDistances).toEqual(['MEDIUM']);
  });

  it('disables every option when the picker is disabled', () => {
    render('NEAR', true);

    expect(radioInputs().every((input) => input.disabled)).toBe(true);
  });

  it('keeps every option enabled when the picker is not disabled', () => {
    render('NEAR');

    expect(radioInputs().some((input) => input.disabled)).toBe(false);
  });
});
