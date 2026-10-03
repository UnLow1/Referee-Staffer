export interface Vacation {
  id: number;
  refereeId: number;
  /**
   * Inclusive range as the backend sends it: ISO-8601 calendar days ("2026-08-01",
   * LocalDate), which is also what the native date inputs bind. Strings, not Dates —
   * they compare lexicographically, which the list and the form both rely on.
   */
  startDate: string;
  endDate: string;
}
