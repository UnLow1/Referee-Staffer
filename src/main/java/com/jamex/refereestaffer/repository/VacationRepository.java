package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Long> {

    // Parameter names say what the bounds mean, not which column they belong to: the derived
    // name reads "startDate <= first argument AND endDate >= second argument", so in an overlap
    // test the range's end bounds startDate and the range's start bounds endDate.
    List<Vacation> findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(LocalDate startDateAtMost,
                                                                                LocalDate endDateAtLeast);

    /**
     * Vacations overlapping the closed range {@code [from, to]}, i.e. {@code startDate <= to}
     * and {@code endDate >= from}. The swap is easy to get backwards and is symmetric for a
     * single day, so it lives here once rather than at every call site — and
     * {@code StafferIntegrationSpec} pins it against real SQL with a multi-day vacation, the
     * only shape that can tell the two argument orders apart.
     */
    default List<Vacation> findAllOverlapping(LocalDate from, LocalDate to) {
        return findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(to, from);
    }
}
