export function alertPromiseTextFor(alertAtPeopleAhead: number): string {
  if (alertAtPeopleAhead === 0) {
    return 'We will tell you when you are next';
  }
  const peopleAheadPhrase =
    alertAtPeopleAhead === 1 ? '1 person is' : `${alertAtPeopleAhead} people are`;
  return `We will tell you when ${peopleAheadPhrase} ahead`;
}
