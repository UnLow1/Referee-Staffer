package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findAllByRefereeIn(Collection<Referee> referees);

    List<Match> findAllByQueue(Short queue);

    // Spelled out instead of derived: H2 (and MySQL) sort NULLs first in ascending order, and
    // the assignment sheet should list the matches still waiting for a date at the end.
    @Query("select m from Match m where m.queue = :queue order by m.date asc nulls last")
    List<Match> findAllByQueueOrderByDateAsc(@Param("queue") Short queue);

    List<Match> findAllByHomeScoreNotNullAndAwayScoreNotNull();

    List<Match> findAllByRefereeInAndDateGreaterThanEqualAndDateLessThan(Collection<Referee> referees,
                                                                         LocalDateTime from, LocalDateTime to);

    default List<Match> findAllByRefereeInAndDateOnDay(Collection<Referee> referees, LocalDateTime dateTime) {
        var dayStart = dateTime.toLocalDate().atStartOfDay();
        return findAllByRefereeInAndDateGreaterThanEqualAndDateLessThan(referees, dayStart, dayStart.plusDays(1));
    }
}
