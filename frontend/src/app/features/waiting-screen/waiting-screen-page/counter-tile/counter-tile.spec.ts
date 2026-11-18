import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CounterOnScreen } from '../../../../core/models/waiting-room.model';
import { CounterTile } from './counter-tile';

const calledCounter: CounterOnScreen = {
  counterName: 'Counter 2',
  serviceName: 'Registration',
  ticketCode: 'R-014',
};

const idleCounter: CounterOnScreen = {
  counterName: 'Counter 3',
  serviceName: 'Documents',
  ticketCode: null,
};

describe('CounterTile', () => {
  let fixture: ComponentFixture<CounterTile>;

  function renderWithCounter(counter: CounterOnScreen): HTMLElement {
    fixture = TestBed.createComponent(CounterTile);
    fixture.componentRef.setInput('counter', counter);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows the counter name, the service name and the ticket code of a called counter', () => {
    const tileElement = renderWithCounter(calledCounter);

    expect(tileElement.querySelector('.counter-name')?.textContent?.trim()).toBe('Counter 2');
    expect(tileElement.querySelector('.service-name')?.textContent?.trim()).toBe('Registration');
    expect(tileElement.querySelector('.ticket-code')?.textContent?.trim()).toBe('R-014');
    expect(tileElement.querySelector('.not-serving')).toBeNull();
  });

  it('marks the tile as called when a ticket is called', () => {
    const tileElement = renderWithCounter(calledCounter);

    expect(tileElement.querySelector('.counter-tile-called')).not.toBeNull();
  });

  it('shows Not serving without a ticket code when no ticket is called', () => {
    const tileElement = renderWithCounter(idleCounter);

    expect(tileElement.querySelector('.not-serving')?.textContent?.trim()).toBe('Not serving');
    expect(tileElement.querySelector('.ticket-code')).toBeNull();
  });

  it('does not mark the tile as called when no ticket is called', () => {
    const tileElement = renderWithCounter(idleCounter);

    expect(tileElement.querySelector('.counter-tile-called')).toBeNull();
  });
});
