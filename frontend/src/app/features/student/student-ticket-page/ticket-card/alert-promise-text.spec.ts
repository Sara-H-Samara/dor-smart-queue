import { alertPromiseTextFor } from './alert-promise-text';

describe('alertPromiseTextFor', () => {
  it('names the number of people for an alert at several people ahead', () => {
    expect(alertPromiseTextFor(6)).toBe('We will tell you when 6 people are ahead');
  });

  it('uses the singular for an alert at one person ahead', () => {
    expect(alertPromiseTextFor(1)).toBe('We will tell you when 1 person is ahead');
  });

  it('says the student is next for an alert at zero people ahead', () => {
    expect(alertPromiseTextFor(0)).toBe('We will tell you when you are next');
  });
});
