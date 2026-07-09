import {Grade, effectiveGradeValue, isSplitGrade} from './grade';

describe('Grade model helpers', () => {
  it('returns the single value for a plain grade', () => {
    const grade: Grade = {id: 1, value: 8.3};

    expect(effectiveGradeValue(grade)).toBe(8.3);
    expect(isSplitGrade(grade)).toBeFalse();
  });

  it('returns the arithmetic mean of both components for a split grade', () => {
    const grade: Grade = {id: 1, value: 7.9, secondValue: 8.3};

    expect(effectiveGradeValue(grade)).toBeCloseTo(8.1, 10);
    expect(isSplitGrade(grade)).toBeTrue();
  });

  it('treats an explicit null second component as a plain grade', () => {
    const grade: Grade = {id: 1, value: 7.9, secondValue: null};

    expect(effectiveGradeValue(grade)).toBe(7.9);
    expect(isSplitGrade(grade)).toBeFalse();
  });
});
