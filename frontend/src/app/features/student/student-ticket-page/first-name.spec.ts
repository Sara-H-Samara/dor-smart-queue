import { firstNameOf } from './first-name';

describe('firstNameOf', () => {
  it('returns the first word of a two-word name', () => {
    expect(firstNameOf('Sara Samara')).toBe('Sara');
  });

  it('ignores spaces around the name', () => {
    expect(firstNameOf('  Sara   Samara  ')).toBe('Sara');
  });

  it('returns empty text for a name that is only spaces', () => {
    expect(firstNameOf('   ')).toBe('');
  });
});
