export interface Vacation {
  id: number;
  refereeId: number;
  startDate: Date;
  endDate: Date;
}

/**
 * Body of POST /api/vacations. `id` is backend-assigned; VacationDto marks every other
 * field `@NotNull`, so a plain `Omit` is the whole create payload.
 */
export type NewVacation = Omit<Vacation, 'id'>;
