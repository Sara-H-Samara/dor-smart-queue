import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CalledStudent } from '../../../../core/models/staff-desk.model';
import { CurrentStudentCard } from './current-student-card';

const calledStudent: CalledStudent = {
  id: 14,
  ticketCode: 'R-014',
  studentName: 'Sample Student',
  status: 'CALLED',
  calledAt: '2026-11-16T09:30:00',
  counterName: 'Counter 2',
};

describe('CurrentStudentCard', () => {
  let fixture: ComponentFixture<CurrentStudentCard>;
  let cardElement: HTMLElement;

  beforeEach(() => {
    fixture = TestBed.createComponent(CurrentStudentCard);
    cardElement = fixture.nativeElement as HTMLElement;
  });

  function buttonWithText(text: string): HTMLButtonElement | undefined {
    return Array.from(cardElement.querySelectorAll('button')).find(
      (button) => button.textContent?.trim() === text,
    );
  }

  it('render_withCalledStudent_showsNameCodeAndCalledChip', () => {
    fixture.componentRef.setInput('student', calledStudent);

    fixture.detectChanges();

    expect(cardElement.querySelector('.service-name')?.textContent?.trim()).toBe('Sample Student');
    expect(cardElement.querySelector('.ticket-code')?.textContent?.trim()).toBe('R-014');
    expect(cardElement.querySelector('.state-chip')?.textContent?.trim()).toBe('Called');
    expect(cardElement.querySelector('article')?.getAttribute('aria-label')).toBe('Ticket R-014');
  });

  it('render_withCounterName_showsCalledToCounterLine', () => {
    fixture.componentRef.setInput('student', calledStudent);

    fixture.detectChanges();

    expect(cardElement.querySelector('.ticket-status b')?.textContent?.trim()).toBe(
      'Called to Counter 2',
    );
  });

  it('render_withoutCounterName_showsCalledLineOnly', () => {
    fixture.componentRef.setInput('student', { ...calledStudent, counterName: null });

    fixture.detectChanges();

    expect(cardElement.querySelector('.ticket-status b')?.textContent?.trim()).toBe('Called');
  });

  it('render_withoutStudent_showsEmptyStateAndNoButtons', () => {
    fixture.componentRef.setInput('student', null);

    fixture.detectChanges();

    expect(cardElement.querySelector('.empty-state-title')?.textContent?.trim()).toBe(
      'No student called yet',
    );
    expect(cardElement.querySelector('article')).toBeNull();
    expect(cardElement.querySelectorAll('button').length).toBe(0);
  });

  it('click_onServed_emitsDoneRequested', () => {
    const doneRequested = vi.fn();
    const noShowRequested = vi.fn();
    fixture.componentInstance.doneRequested.subscribe(doneRequested);
    fixture.componentInstance.noShowRequested.subscribe(noShowRequested);
    fixture.componentRef.setInput('student', calledStudent);
    fixture.detectChanges();

    buttonWithText('Served')?.click();

    expect(doneRequested).toHaveBeenCalledTimes(1);
    expect(noShowRequested).not.toHaveBeenCalled();
  });

  it('click_onNoShow_emitsNoShowRequested', () => {
    const doneRequested = vi.fn();
    const noShowRequested = vi.fn();
    fixture.componentInstance.doneRequested.subscribe(doneRequested);
    fixture.componentInstance.noShowRequested.subscribe(noShowRequested);
    fixture.componentRef.setInput('student', calledStudent);
    fixture.detectChanges();

    buttonWithText('No-show')?.click();

    expect(noShowRequested).toHaveBeenCalledTimes(1);
    expect(doneRequested).not.toHaveBeenCalled();
  });

  it('render_whileSubmitting_disablesBothButtons', () => {
    fixture.componentRef.setInput('student', calledStudent);
    fixture.componentRef.setInput('isSubmitting', true);

    fixture.detectChanges();

    expect(buttonWithText('Served')?.disabled).toBe(true);
    expect(buttonWithText('No-show')?.disabled).toBe(true);
  });

  it('render_whenNotSubmitting_enablesBothButtons', () => {
    fixture.componentRef.setInput('student', calledStudent);

    fixture.detectChanges();

    expect(buttonWithText('Served')?.disabled).toBe(false);
    expect(buttonWithText('No-show')?.disabled).toBe(false);
  });
});
