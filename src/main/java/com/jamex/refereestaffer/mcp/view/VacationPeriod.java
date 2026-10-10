package com.jamex.refereestaffer.mcp.view;

import java.time.LocalDate;

/** A referee's unavailability window. Both bounds are inclusive. */
public record VacationPeriod(
        Long id,
        LocalDate startDate,
        LocalDate endDate
) {
}
