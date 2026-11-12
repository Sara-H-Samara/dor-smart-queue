import { firstNameOf } from './first-name';

describe('firstNameOf', () => {
  it('firstNameOf_withTwoWords_returnsFirstWord', () => {
    expect(firstNameOf('Sara Samara')).toBe('Sara');
  });

  it('firstNameOf_withSurroundingSpaces_returnsFirstWord', () => {
    expect(firstNameOf('  Sara   Samara  ')).toBe('Sara');
  });

  it('firstNameOf_withEmptyText_returnsEmptyText', () => {
    expect(firstNameOf('   ')).toBe('');
  });
});
