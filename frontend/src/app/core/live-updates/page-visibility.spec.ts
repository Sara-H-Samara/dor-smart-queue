import { TestBed } from '@angular/core/testing';

import { PageVisibility } from './page-visibility';

function showDocumentAs(visibilityState: DocumentVisibilityState): void {
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: () => visibilityState,
  });
  document.dispatchEvent(new Event('visibilitychange'));
}

describe('PageVisibility', () => {
  afterEach(() => {
    Reflect.deleteProperty(document, 'visibilityState');
  });

  it('starts as visible when the tab is visible', () => {
    showDocumentAs('visible');

    expect(TestBed.inject(PageVisibility).isVisible()).toBe(true);
  });

  it('starts as not visible when the tab is hidden', () => {
    showDocumentAs('hidden');

    expect(TestBed.inject(PageVisibility).isVisible()).toBe(false);
  });

  it('becomes not visible when the tab is hidden', () => {
    showDocumentAs('visible');
    const pageVisibility = TestBed.inject(PageVisibility);

    showDocumentAs('hidden');

    expect(pageVisibility.isVisible()).toBe(false);
  });

  it('becomes visible again when the tab is shown again', () => {
    showDocumentAs('hidden');
    const pageVisibility = TestBed.inject(PageVisibility);

    showDocumentAs('visible');

    expect(pageVisibility.isVisible()).toBe(true);
  });
});
