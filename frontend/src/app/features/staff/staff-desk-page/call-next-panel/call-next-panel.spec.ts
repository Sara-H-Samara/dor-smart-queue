import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CallNextPanel } from './call-next-panel';

describe('CallNextPanel', () => {
  let fixture: ComponentFixture<CallNextPanel>;
  let panelElement: HTMLElement;

  beforeEach(() => {
    fixture = TestBed.createComponent(CallNextPanel);
    fixture.componentRef.setInput('canCallNext', true);
    panelElement = fixture.nativeElement as HTMLElement;
  });

  function callNextButton(): HTMLButtonElement {
    return panelElement.querySelector<HTMLButtonElement>('.call-next-button')!;
  }

  it('enables the button when calling next is allowed', () => {
    fixture.detectChanges();

    expect(callNextButton().textContent?.trim()).toBe('Call next');
    expect(callNextButton().disabled).toBe(false);
  });

  it('disables the button when calling next is not allowed', () => {
    fixture.componentRef.setInput('canCallNext', false);

    fixture.detectChanges();

    expect(callNextButton().disabled).toBe(true);
  });

  it('disables the button while a call is running', () => {
    fixture.componentRef.setInput('isCalling', true);

    fixture.detectChanges();

    expect(callNextButton().disabled).toBe(true);
  });

  it('shows how many are waiting for the service', () => {
    fixture.componentRef.setInput('waitingCount', 3);
    fixture.componentRef.setInput('serviceName', 'Course registration');

    fixture.detectChanges();

    expect(panelElement.querySelector('.waiting-count')?.textContent?.trim()).toBe(
      '3 waiting for Course registration',
    );
  });

  it('shows zero waiting and keeps the button enabled', () => {
    fixture.componentRef.setInput('waitingCount', 0);
    fixture.componentRef.setInput('serviceName', 'Course registration');

    fixture.detectChanges();

    expect(panelElement.querySelector('.waiting-count')?.textContent?.trim()).toBe(
      '0 waiting for Course registration',
    );
    expect(callNextButton().disabled).toBe(false);
  });

  it('shows no waiting line when the waiting count is unknown', () => {
    fixture.detectChanges();

    expect(panelElement.querySelector('.waiting-count')).toBeNull();
  });

  it('emits a call-next request when the button is clicked', () => {
    const callNextRequested = vi.fn();
    fixture.componentInstance.callNextRequested.subscribe(callNextRequested);
    fixture.detectChanges();

    callNextButton().click();

    expect(callNextRequested).toHaveBeenCalledTimes(1);
  });
});
