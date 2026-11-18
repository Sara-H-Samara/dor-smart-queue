import { TestBed } from '@angular/core/testing';

import { WaitingScreenHeader } from './waiting-screen-header';

describe('WaitingScreenHeader', () => {
  function renderWithOrganizationName(organizationName: string): HTMLElement {
    const fixture = TestBed.createComponent(WaitingScreenHeader);
    fixture.componentRef.setInput('organizationName', organizationName);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows the organization name', () => {
    const headerElement = renderWithOrganizationName('Student Affairs office');

    expect(headerElement.querySelector('.organization-name')?.textContent?.trim()).toBe(
      'Student Affairs office',
    );
  });

  it('shows the Dor wordmark with an accessible name', () => {
    const headerElement = renderWithOrganizationName('Student Affairs office');

    expect(headerElement.querySelector('.wordmark')?.getAttribute('aria-label')).toBe('Dor');
  });
});
