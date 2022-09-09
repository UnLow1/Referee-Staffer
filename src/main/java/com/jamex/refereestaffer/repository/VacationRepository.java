package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Long> {

    List<Vacation> findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(LocalDate startDate, LocalDate endDate);

    default List<Vacation> findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(LocalDateTime dateTime) {
        var date = dateTime.toLocalDate();
        return findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(date, date);
    }
}
