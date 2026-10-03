export interface Grade {
  id: number;
  value: number;
  /** Second component of a split grade (e.g. 7.9/8.3); absent for a plain grade. */
  secondValue?: number | null;
}

/**
 * Body of POST /api/grades/{matchId}. `id` is backend-assigned; GradeDto requires `value`
 * and leaves the second component of a split grade optional.
 */
export type NewGrade = Omit<Grade, 'id'>;

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
