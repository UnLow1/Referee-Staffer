import {
  Grade,
  NO_VALUE,
  effectiveGradeValue,
  formatAverageGrade,
  formatObserverGrade,
  isSplitGrade,
} from './grade';

describe('Grade model helpers', () => {
  it('returns the single value for a plain grade', () => {
    const grade: Grade = {id: 1, value: 8.3};

    expect(effectiveGradeValue(grade)).toBe(8.3);
    expect(isSplitGrade(grade)).toBe(false);
  });

  it('returns the arithmetic mean of both components for a split grade', () => {
    const grade: Grade = {id: 1, value: 7.9, secondValue: 8.3};

    expect(effectiveGradeValue(grade)).toBeCloseTo(8.1, 10);
    expect(isSplitGrade(grade)).toBe(true);
  });

  it('treats an explicit null second component as a plain grade', () => {
    const grade: Grade = {id: 1, value: 7.9, secondValue: null};

    expect(effectiveGradeValue(grade)).toBe(7.9);
    expect(isSplitGrade(grade)).toBe(false);
  });
});

describe('formatAverageGrade', () => {
  it('keeps referees with averages inside a tenth of each other distinguishable', () => {
    // The real season data from RS-114: at one decimal all four rendered as "8.3".
    const measured = [8.330769230769231, 8.326666666666666, 8.3, 8.266666666666667];

    const rendered = measured.map(formatAverageGrade);

    expect(rendered).toEqual(['8.331', '8.327', '8.300', '8.267']);
    expect(new Set(rendered).size).toBe(measured.length);
  });

  it('renders a dash instead of a number when the referee has no grades', () => {
    expect(formatAverageGrade(null)).toBe(NO_VALUE);
    expect(formatAverageGrade(undefined)).toBe(NO_VALUE);
  });

  it('pads to a fixed width so the column stays aligned', () => {
    expect(formatAverageGrade(8)).toBe('8.000');
    expect(formatAverageGrade(8.1)).toBe('8.100');
  });
});

describe('formatObserverGrade', () => {
  it('keeps a single grade at the granularity it was awarded with', () => {
    expect(formatObserverGrade(8.3)).toBe('8.3');
    expect(formatObserverGrade(8.100000000000001)).toBe('8.1');
  });
});
