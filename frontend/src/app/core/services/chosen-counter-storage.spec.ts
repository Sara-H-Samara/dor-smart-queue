import { TestBed } from '@angular/core/testing';

import { ChosenCounterStorage } from './chosen-counter-storage';

const STORAGE_KEY = 'dor_staff_counter_id';

function stubLocalStorage(initialValue: string | null = null) {
  const storedValues = new Map<string, string>();
  if (initialValue !== null) {
    storedValues.set(STORAGE_KEY, initialValue);
  }
  const storage = {
    getItem: vi.fn((key: string) => storedValues.get(key) ?? null),
    setItem: vi.fn((key: string, value: string) => void storedValues.set(key, value)),
    removeItem: vi.fn((key: string) => void storedValues.delete(key)),
  };
  vi.stubGlobal('localStorage', storage);
  return storage;
}

function blockStorage(storage: ReturnType<typeof stubLocalStorage>): void {
  const blocked = () => {
    throw new Error('blocked');
  };
  storage.getItem.mockImplementation(blocked);
  storage.setItem.mockImplementation(blocked);
  storage.removeItem.mockImplementation(blocked);
}

describe('ChosenCounterStorage', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('returns null when nothing was saved', () => {
    stubLocalStorage();

    expect(TestBed.inject(ChosenCounterStorage).read()).toBeNull();
  });

  it('returns the counter id that was saved', () => {
    stubLocalStorage();
    const chosenCounterStorage = TestBed.inject(ChosenCounterStorage);

    chosenCounterStorage.save(7);

    expect(chosenCounterStorage.read()).toBe(7);
  });

  it('returns null after the saved counter is cleared', () => {
    stubLocalStorage();
    const chosenCounterStorage = TestBed.inject(ChosenCounterStorage);

    chosenCounterStorage.save(7);
    chosenCounterStorage.clear();

    expect(chosenCounterStorage.read()).toBeNull();
  });

  it.each(['abc', '', '0', '-3', '2.5'])(
    'returns null when the stored value is %j',
    (storedValue) => {
      stubLocalStorage(storedValue);

      expect(TestBed.inject(ChosenCounterStorage).read()).toBeNull();
    },
  );

  it('returns null when the browser blocks storage', () => {
    blockStorage(stubLocalStorage());

    expect(TestBed.inject(ChosenCounterStorage).read()).toBeNull();
  });

  it('does not throw when saving while the browser blocks storage', () => {
    blockStorage(stubLocalStorage());

    expect(() => TestBed.inject(ChosenCounterStorage).save(7)).not.toThrow();
  });

  it('does not throw when clearing while the browser blocks storage', () => {
    blockStorage(stubLocalStorage());

    expect(() => TestBed.inject(ChosenCounterStorage).clear()).not.toThrow();
  });
});
