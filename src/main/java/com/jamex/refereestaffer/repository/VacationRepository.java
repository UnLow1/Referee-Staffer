package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Long> {

    List<Vacation> findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(LocalDate startDate, LocalDate endDate);

    /**
     * Vacations overlapping the closed range {@code [from, to]}, i.e. {@code startDate <= to}
     * and {@code endDate >= from}. The derived method above reads as "start before the first
     * argument, end after the second", so the arguments have to be passed swapped — hence this
     * wrapper rather than the raw call at every site.
     */
    default List<Vacation> findAllOverlapping(LocalDate from, LocalDate to) {
        return findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(to, from);
    }
}
