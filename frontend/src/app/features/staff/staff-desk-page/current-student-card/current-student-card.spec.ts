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

  it('shows the name, code and called chip for a called student', () => {
    fixture.componentRef.setInput('student', calledStudent);

    fixture.detectChanges();

    expect(cardElement.querySelector('.service-name')?.textContent?.trim()).toBe('Sample Student');
    expect(cardElement.querySelector('.ticket-code')?.textContent?.trim()).toBe('R-014');
    expect(cardElement.querySelector('.state-chip')?.textContent?.trim()).toBe('Called');
    expect(cardElement.querySelector('article')?.getAttribute('aria-label')).toBe('Ticket R-014');
  });

  it('shows the called-to-counter line when the counter has a name', () => {
    fixture.componentRef.setInput('student', calledStudent);

    fixture.detectChanges();

    expect(cardElement.querySelector('.ticket-status b')?.textContent?.trim()).toBe(
      'Called to Counter 2',
    );
  });

  it('shows only the called line when the counter has no name', () => {
    fixture.componentRef.setInput('student', { ...calledStudent, counterName: null });

    fixture.detectChanges();

    expect(cardElement.querySelector('.ticket-status b')?.textContent?.trim()).toBe('Called');
  });

  it('shows the idle line on a quiet ticket when nobody is called', () => {
    fixture.componentRef.setInput('student', null);

    fixture.detectChanges();

    expect(cardElement.querySelector('.idle-ticket-message')?.textContent?.trim()).toBe(
      'No student at your desk',
    );
    expect(cardElement.querySelector('.idle-ticket app-icon')).toBeNull();
    expect(cardElement.querySelector('.empty-state')).toBeNull();
    expect(cardElement.querySelector('article')).toBeNull();
  });

  it('shows the idle ticket without a stub when nobody is called', () => {
    fixture.componentRef.setInput('student', null);

    fixture.detectChanges();

    expect(cardElement.querySelector('.idle-ticket')).not.toBeNull();
    expect(cardElement.querySelector('.ticket-stub')).toBeNull();
  });

  it('shows the stub with two outcome buttons for a called student', () => {
    fixture.componentRef.setInput('student', calledStudent);

    fixture.detectChanges();

    expect(cardElement.querySelector('.ticket-stub')).not.toBeNull();
    expect(cardElement.querySelectorAll('.ticket-stub button')).toHaveLength(2);
  });

  it('shows no buttons when nobody is called', () => {
    fixture.componentRef.setInput('student', null);

    fixture.detectChanges();

    expect(cardElement.querySelectorAll('button').length).toBe(0);
  });

  it('shows the ticket and no idle line for a called student', () => {
    fixture.componentRef.setInput('student', calledStudent);

    fixture.detectChanges();

    expect(cardElement.querySelector('.idle-ticket-message')).toBeNull();
    expect(cardElement.querySelector('.ticket-code')?.textContent?.trim()).toBe('R-014');
  });

  it('emits a done request when Served is clicked', () => {
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

  it('emits a no-show request when No-show is clicked', () => {
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

  it('disables both buttons while submitting', () => {
    fixture.componentRef.setInput('student', calledStudent);
    fixture.componentRef.setInput('isSubmitting', true);

    fixture.detectChanges();

    expect(buttonWithText('Served')?.disabled).toBe(true);
    expect(buttonWithText('No-show')?.disabled).toBe(true);
  });

  it('enables both buttons when not submitting', () => {
    fixture.componentRef.setInput('student', calledStudent);

    fixture.detectChanges();

    expect(buttonWithText('Served')?.disabled).toBe(false);
    expect(buttonWithText('No-show')?.disabled).toBe(false);
  });
});
