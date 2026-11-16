import { Injectable } from '@angular/core';

const CHOSEN_COUNTER_STORAGE_KEY = 'dor_staff_counter_id';

@Injectable({ providedIn: 'root' })
export class ChosenCounterStorage {
  read(): number | null {
    let storedText: string | null;
    try {
      storedText = localStorage.getItem(CHOSEN_COUNTER_STORAGE_KEY);
    } catch {
      return null;
    }
    const counterId = Number(storedText);
    return storedText !== null && Number.isInteger(counterId) && counterId > 0 ? counterId : null;
  }

  save(counterId: number): void {
    try {
      localStorage.setItem(CHOSEN_COUNTER_STORAGE_KEY, String(counterId));
    } catch {
      // Storage is blocked: the choice still works until the page reloads.
    }
  }

  clear(): void {
    try {
      localStorage.removeItem(CHOSEN_COUNTER_STORAGE_KEY);
    } catch {
      // Storage is blocked: there is nothing stored to clear.
    }
  }
}
