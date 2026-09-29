export interface Grade {
  id: number;
  value: number;
  /** Second component of a split grade (e.g. 7.9/8.3); absent for a plain grade. */
  secondValue?: number | null;
}

/**
 * The grade that counts towards referee statistics: the arithmetic mean of both
 * components of a split grade (7.9/8.3 -> 8.1), or the single value otherwise.
 * Mirrors Grade.getEffectiveValue() on the backend.
 */
export function effectiveGradeValue(grade: Grade): number {
  return grade.secondValue != null ? (grade.value + grade.secondValue) / 2 : grade.value;
}

export function isSplitGrade(grade: Grade): boolean {
  return grade.secondValue != null;
}

/**
 * Decimal places for a *single* observer grade. Observers award in steps of 0.1
 * (RS-44), so anything beyond one decimal is noise.
 */
const OBSERVER_GRADE_DECIMALS = 1;

/**
 * Decimal places for an *averaged* grade. Referee averages across a full season sit
 * within a tenth of each other (RS-114: 12 of 19 referees rendered as `8.3`), so one
 * decimal makes candidates indistinguishable exactly where they must be compared.
 */
const AVERAGE_GRADE_DECIMALS = 3;

/** Rendered when a value is missing — a referee with no graded match, for instance. */
export const NO_VALUE = '—';

/**
 * A single observer grade, at the granularity it was awarded with.
 * Use {@link formatAverageGrade} for a mean of several grades.
 */
export function formatObserverGrade(value: number): string {
  return value.toFixed(OBSERVER_GRADE_DECIMALS);
}

/**
 * A mean of several observer grades, at the precision that keeps referees
 * distinguishable. Null/undefined renders as {@link NO_VALUE}: a referee with no grades
 * must not be shown a number at all — the backend leaves averageGrade unset for them
 * and substitutes its default only inside the scoring formulas (RS-114).
 */
export function formatAverageGrade(value: number | null | undefined): string {
  return value != null ? value.toFixed(AVERAGE_GRADE_DECIMALS) : NO_VALUE;
}
