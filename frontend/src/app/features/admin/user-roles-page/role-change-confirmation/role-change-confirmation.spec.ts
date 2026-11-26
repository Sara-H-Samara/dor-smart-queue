import { ComponentFixture, TestBed } from '@angular/core/testing';

import { installDialogSupport } from '../../../../testing/dialog-support';
import { PendingRoleChange, RoleChangeConfirmation } from './role-change-confirmation';

const makeAnnAdmin: PendingRoleChange = { fullName: 'Ann Lee', oldRole: 'STAFF', newRole: 'ADMIN' };
const removeBobAdmin: PendingRoleChange = {
  fullName: 'Bob Stone',
  oldRole: 'ADMIN',
  newRole: 'STAFF',
};

describe('RoleChangeConfirmation', () => {
  let fixture: ComponentFixture<RoleChangeConfirmation>;
  let hostElement: HTMLElement;
  let emittedEvents: string[];

  beforeAll(() => installDialogSupport());

  beforeEach(() => {
    fixture = TestBed.createComponent(RoleChangeConfirmation);
    hostElement = fixture.nativeElement as HTMLElement;
    document.body.appendChild(hostElement);
    emittedEvents = [];
    fixture.componentInstance.confirmed.subscribe(() => emittedEvents.push('confirmed'));
    fixture.componentInstance.cancelled.subscribe(() => emittedEvents.push('cancelled'));
  });

  afterEach(() => hostElement.remove());

  function openWith(change: PendingRoleChange): void {
    fixture.componentRef.setInput('pendingChange', change);
    fixture.detectChanges();
  }

  function confirmationElement(): HTMLDialogElement {
    return hostElement.querySelector('dialog')!;
  }

  function textOf(selector: string): string | undefined {
    return hostElement.querySelector(selector)?.textContent?.trim();
  }

  function useBox(element: HTMLElement, left: number, top: number, right: number, bottom: number) {
    element.getBoundingClientRect = () =>
      ({ left, top, right, bottom, width: right - left, height: bottom - top }) as DOMRect;
  }

  function buttons(): HTMLButtonElement[] {
    return Array.from(hostElement.querySelectorAll<HTMLButtonElement>('button'));
  }

  it('stays closed while no change is pending', () => {
    fixture.detectChanges();

    expect(confirmationElement().hasAttribute('open')).toBe(false);
  });

  it('shows the make admin text when the new role is admin', () => {
    openWith(makeAnnAdmin);

    expect(confirmationElement().hasAttribute('open')).toBe(true);
    expect(textOf('h3')).toBe('Make Ann Lee an admin?');
    const paragraphs = Array.from(hostElement.querySelectorAll('p')).map((paragraph) =>
      paragraph.textContent?.trim(),
    );
    expect(paragraphs).toEqual([
      'Admins can change services, counters and the roles of all users, including other admins.',
      'Ann Lee must sign in again to get admin rights.',
    ]);
    expect(buttons().map((button) => button.textContent?.trim())).toEqual(['Cancel', 'Make admin']);
  });

  it('shows the remove admin text when the old role is admin', () => {
    openWith(removeBobAdmin);

    expect(textOf('h3')).toBe('Remove admin rights from Bob Stone?');
    const paragraphs = Array.from(hostElement.querySelectorAll('p')).map((paragraph) =>
      paragraph.textContent?.trim(),
    );
    expect(paragraphs).toEqual([
      'Bob Stone will be Staff and can no longer use the admin pages.',
      'Bob Stone must sign in again.',
    ]);
    expect(buttons()[1].textContent?.trim()).toBe('Remove admin rights');
  });

  it('puts the focus on cancel when it opens', () => {
    openWith(makeAnnAdmin);

    expect(document.activeElement).toBe(buttons()[0]);
  });

  it('emits confirmed when the confirm button is pressed', () => {
    openWith(makeAnnAdmin);

    buttons()[1].click();

    expect(emittedEvents).toEqual(['confirmed']);
    expect(confirmationElement().hasAttribute('open')).toBe(false);
  });

  it('emits cancelled when cancel is pressed', () => {
    openWith(makeAnnAdmin);

    buttons()[0].click();

    expect(emittedEvents).toEqual(['cancelled']);
    expect(confirmationElement().hasAttribute('open')).toBe(false);
  });

  it('emits cancelled when escape is pressed', () => {
    openWith(removeBobAdmin);

    confirmationElement().dispatchEvent(new Event('cancel', { cancelable: true }));

    expect(emittedEvents).toEqual(['cancelled']);
    expect(confirmationElement().hasAttribute('open')).toBe(false);
  });

  it('emits cancelled when the area outside the box is clicked', () => {
    openWith(makeAnnAdmin);
    useBox(confirmationElement(), 100, 100, 300, 200);

    confirmationElement().dispatchEvent(
      new MouseEvent('click', { bubbles: true, clientX: 20, clientY: 20 }),
    );

    expect(emittedEvents).toEqual(['cancelled']);
    expect(confirmationElement().hasAttribute('open')).toBe(false);
  });

  it('stays open when the space inside the box is clicked', () => {
    openWith(makeAnnAdmin);
    useBox(confirmationElement(), 100, 100, 300, 200);

    confirmationElement().dispatchEvent(
      new MouseEvent('click', { bubbles: true, clientX: 150, clientY: 150 }),
    );
    hostElement.querySelector('p')?.click();

    expect(emittedEvents).toEqual([]);
    expect(confirmationElement().hasAttribute('open')).toBe(true);
  });

  it('emits cancelled after a confirmed change when the next one is cancelled', () => {
    openWith(makeAnnAdmin);
    buttons()[1].click();
    fixture.componentRef.setInput('pendingChange', null);
    fixture.detectChanges();

    openWith(removeBobAdmin);
    buttons()[0].click();

    expect(emittedEvents).toEqual(['confirmed', 'cancelled']);
  });
});
