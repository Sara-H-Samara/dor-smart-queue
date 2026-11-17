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

  it('render_whenCallNextIsAllowed_enablesButton', () => {
    fixture.detectChanges();

    expect(callNextButton().textContent?.trim()).toBe('Call next');
    expect(callNextButton().disabled).toBe(false);
  });

  it('render_whenCallNextIsNotAllowed_disablesButton', () => {
    fixture.componentRef.setInput('canCallNext', false);

    fixture.detectChanges();

    expect(callNextButton().disabled).toBe(true);
  });

  it('render_whileCalling_disablesButton', () => {
    fixture.componentRef.setInput('isCalling', true);

    fixture.detectChanges();

    expect(callNextButton().disabled).toBe(true);
  });

  it('render_withWaitingCountAndServiceName_showsWaitingForServiceLine', () => {
    fixture.componentRef.setInput('waitingCount', 3);
    fixture.componentRef.setInput('serviceName', 'Course registration');

    fixture.detectChanges();

    expect(panelElement.querySelector('.waiting-count')?.textContent?.trim()).toBe(
      '3 waiting for Course registration',
    );
  });

  it('render_withZeroWaiting_showsZeroLineAndKeepsButtonEnabled', () => {
    fixture.componentRef.setInput('waitingCount', 0);
    fixture.componentRef.setInput('serviceName', 'Course registration');

    fixture.detectChanges();

    expect(panelElement.querySelector('.waiting-count')?.textContent?.trim()).toBe(
      '0 waiting for Course registration',
    );
    expect(callNextButton().disabled).toBe(false);
  });

  it('render_withNullWaitingCount_showsNoCountLine', () => {
    fixture.detectChanges();

    expect(panelElement.querySelector('.waiting-count')).toBeNull();
  });

  it('click_whenCallNextIsAllowed_emitsCallNextRequested', () => {
    const callNextRequested = vi.fn();
    fixture.componentInstance.callNextRequested.subscribe(callNextRequested);
    fixture.detectChanges();

    callNextButton().click();

    expect(callNextRequested).toHaveBeenCalledTimes(1);
  });
});
