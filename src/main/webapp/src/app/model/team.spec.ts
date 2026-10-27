import {deriveShortCode, SHORT_CODE_PATTERN, shortCodeOf, Team} from './team';

describe('team short codes', () => {
  describe('deriveShortCode', () => {
    it.each([
      ['Legia', 'LEG'],
      ['korona', 'KOR'],
      ['FC Barcelona', 'FCB'],
      ['A.C. Milan', 'ACM'],
      ['Lech-Poznan', 'LEC'],
      ['AC', 'AC'],
      ['Śląsk', 'ŚLĄ'],
      ['', ''],
      ['???', '']
    ])('derives %o as %o, matching Team.deriveShortCode in Java', (name, expected) => {
      expect(deriveShortCode(name)).toBe(expected);
    });

    it('treats a missing name as no code rather than throwing', () => {
      expect(deriveShortCode(undefined)).toBe('');
      expect(deriveShortCode(null)).toBe('');
    });
  });

  describe('shortCodeOf', () => {
    const team: Team = {id: 1, name: 'Lechia', city: 'Gdansk', points: 0};

    it('prefers the code the backend computed', () => {
      expect(shortCodeOf({...team, short: 'LGA'})).toBe('LGA');
    });

    it('falls back to the name when the field is absent', () => {
      expect(shortCodeOf(team)).toBe('LEC');
    });

    it('renders nothing for a missing team', () => {
      expect(shortCodeOf(undefined)).toBe('');
      expect(shortCodeOf(null)).toBe('');
    });
  });

  describe('SHORT_CODE_PATTERN', () => {
    // Guards the `u` flag: without it `\p{L}` degrades to an identity escape and the pattern
    // rejects every ordinary code, which is exactly how a template binding broke once.
    it.each(['LGA', 'ŁKS', 'ŚLĄ', 'LG1', '16', ''])('accepts %o', value => {
      expect(SHORT_CODE_PATTERN.test(value)).toBe(true);
    });

    it.each(['LG-', 'LG A', 'LG.', 'L_G'])('rejects %o', value => {
      expect(SHORT_CODE_PATTERN.test(value)).toBe(false);
    });
  });
});
