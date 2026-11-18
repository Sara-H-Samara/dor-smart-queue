import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageHeader } from './page-header';

describe('PageHeader', () => {
  function renderHeader(
    inputs: { contextText?: string; secondaryActionLabel?: string; showSignOut?: boolean } = {},
  ): ComponentFixture<PageHeader> {
    const fixture = TestBed.createComponent(PageHeader);
    for (const [inputName, inputValue] of Object.entries(inputs)) {
      fixture.componentRef.setInput(inputName, inputValue);
    }
    fixture.detectChanges();
    return fixture;
  }

  function headerButtons(fixture: ComponentFixture<PageHeader>): HTMLButtonElement[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'));
  }

  function buttonLabels(fixture: ComponentFixture<PageHeader>): string[] {
    return headerButtons(fixture).map((button) => button.textContent?.trim() ?? '');
  }

  function buttonWithLabel(fixture: ComponentFixture<PageHeader>, label: string): HTMLButtonElement {
    const matchingButton = headerButtons(fixture).find(
      (button) => button.textContent?.trim() === label,
    );
    if (!matchingButton) {
      throw new Error(`No header button labelled ${label}`);
    }
    return matchingButton;
  }

  it('shows the Dor wordmark with an accessible name', () => {
    const fixture = renderHeader();

    const wordmark = (fixture.nativeElement as HTMLElement).querySelector('.wordmark');

    expect(wordmark?.getAttribute('role')).toBe('img');
    expect(wordmark?.getAttribute('aria-label')).toBe('Dor');
  });

  it('hides the sign-out button when sign-out is not enabled', () => {
    const fixture = renderHeader();

    expect(buttonLabels(fixture)).toEqual([]);
  });

  it('shows the sign-out button when sign-out is enabled', () => {
    const fixture = renderHeader({ showSignOut: true });

    expect(buttonLabels(fixture)).toEqual(['Sign out']);
  });

  it('asks the page to sign out when the sign-out button is clicked', () => {
    const fixture = renderHeader({ showSignOut: true });
    const signOutRequests: unknown[] = [];
    fixture.componentInstance.signOutRequested.subscribe((request) => signOutRequests.push(request));

    buttonWithLabel(fixture, 'Sign out').click();

    expect(signOutRequests.length).toBe(1);
  });

  it('shows the context text when it is given', () => {
    const fixture = renderHeader({ contextText: 'Window 2 · Fees' });

    const contextElement = (fixture.nativeElement as HTMLElement).querySelector(
      '.page-header-context',
    );

    expect(contextElement?.textContent?.trim()).toBe('Window 2 · Fees');
  });

  it('hides the context text when it is not given', () => {
    const fixture = renderHeader();

    expect((fixture.nativeElement as HTMLElement).querySelector('.page-header-context')).toBeNull();
  });

  it('hides the secondary action button when no label is given', () => {
    const fixture = renderHeader({ showSignOut: true });

    expect(buttonLabels(fixture)).not.toContain('Change counter');
  });

  it('shows the secondary action button before the sign-out button', () => {
    const fixture = renderHeader({ secondaryActionLabel: 'Change counter', showSignOut: true });

    expect(buttonLabels(fixture)).toEqual(['Change counter', 'Sign out']);
  });

  it('shows an icon inside each header button', () => {
    const fixture = renderHeader({ secondaryActionLabel: 'Change counter', showSignOut: true });

    const buttonsWithoutIcon = headerButtons(fixture).filter(
      (button) => button.querySelector('app-icon svg') === null,
    );

    expect(buttonsWithoutIcon.length).toBe(0);
  });

  it('hides the button icons from screen readers', () => {
    const fixture = renderHeader({ secondaryActionLabel: 'Change counter', showSignOut: true });

    const icons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button app-icon'),
    );

    expect(icons.map((icon) => icon.getAttribute('aria-hidden'))).toEqual(['true', 'true']);
  });

  it('asks the page to run the secondary action when its button is clicked', () => {
    const fixture = renderHeader({ secondaryActionLabel: 'Change counter' });
    const secondaryActionRequests: unknown[] = [];
    fixture.componentInstance.secondaryActionRequested.subscribe((request) =>
      secondaryActionRequests.push(request),
    );

    buttonWithLabel(fixture, 'Change counter').click();

    expect(secondaryActionRequests.length).toBe(1);
  });
});
