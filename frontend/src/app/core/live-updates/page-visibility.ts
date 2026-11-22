import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class PageVisibility {
  private readonly document = inject(DOCUMENT);
  private readonly documentIsVisible = signal(this.isDocumentVisible());

  readonly isVisible = this.documentIsVisible.asReadonly();

  constructor() {
    this.document.addEventListener('visibilitychange', () =>
      this.documentIsVisible.set(this.isDocumentVisible()),
    );
  }

  private isDocumentVisible(): boolean {
    return this.document.visibilityState !== 'hidden';
  }
}
