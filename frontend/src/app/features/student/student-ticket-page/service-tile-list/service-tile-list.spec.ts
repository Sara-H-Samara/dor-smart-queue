import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ActiveService } from '../../../../core/models/active-service.model';
import { ServiceTileList } from './service-tile-list';

const serviceWithOpenCounter: ActiveService = {
  id: 1,
  name: 'Registration',
  code: 'R',
  active: true,
  hasOpenCounter: true,
};

const serviceWithoutOpenCounter: ActiveService = {
  id: 2,
  name: 'Documents',
  code: 'D',
  active: true,
  hasOpenCounter: false,
};

describe('ServiceTileList', () => {
  let fixture: ComponentFixture<ServiceTileList>;
  let chosenServices: ActiveService[];

  function renderWithServices(services: ActiveService[], isTakingTicket = false): void {
    fixture = TestBed.createComponent(ServiceTileList);
    chosenServices = [];
    fixture.componentInstance.serviceChosen.subscribe((service) => chosenServices.push(service));
    fixture.componentRef.setInput('services', services);
    fixture.componentRef.setInput('isTakingTicket', isTakingTicket);
    fixture.detectChanges();
  }

  function tileButtons(): HTMLButtonElement[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'));
  }

  it('render_serviceWithoutOpenCounter_showsNoCounterLineAndAriaDisabled', () => {
    renderWithServices([serviceWithoutOpenCounter]);

    const tileButton = tileButtons()[0];
    const noteId = tileButton.getAttribute('aria-describedby');
    const noteElement = (fixture.nativeElement as HTMLElement).querySelector(`#${noteId}`);
    expect(noteId).toBe('service-note-2');
    expect(noteElement?.textContent?.trim()).toBe('No counter open right now');
    expect(tileButton.getAttribute('aria-disabled')).toBe('true');
    expect(tileButton.disabled).toBe(false);
  });

  it('chooseService_serviceWithoutOpenCounter_emitsNothing', () => {
    renderWithServices([serviceWithoutOpenCounter]);

    tileButtons()[0].click();

    expect(chosenServices).toEqual([]);
  });

  it('chooseService_serviceWithOpenCounter_emitsService', () => {
    renderWithServices([serviceWithOpenCounter]);

    tileButtons()[0].click();

    expect(chosenServices).toEqual([serviceWithOpenCounter]);
  });

  it('render_serviceWithOpenCounter_showsNoNoteAndNoAriaAttributes', () => {
    renderWithServices([serviceWithOpenCounter]);

    const tileButton = tileButtons()[0];
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('No counter open');
    expect(tileButton.hasAttribute('aria-disabled')).toBe(false);
    expect(tileButton.hasAttribute('aria-describedby')).toBe(false);
  });

  it('render_whileTakingTicket_disablesEveryTileNatively', () => {
    renderWithServices([serviceWithOpenCounter, serviceWithoutOpenCounter], true);

    expect(tileButtons().map((tileButton) => tileButton.disabled)).toEqual([true, true]);
  });
});
