package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findAllByRefereeIn(Collection<Referee> referees);

    List<Match> findAllByQueue(Short queue);

    boolean existsByQueue(Short queue);

    /**
     * The queues the season actually has, ascending. Drives the Staffer's queue stepper
     * (RS-115) so it cannot be walked past the end of the season — counting distinct
     * queues server-side beats shipping every match row to the browser for the same answer.
     */
    @Query("select distinct m.queue from Match m order by m.queue")
    List<Short> findDistinctQueues();

    List<Match> findAllByQueueOrderByDateAsc(Short queue);

    List<Match> findAllByHomeScoreNotNullAndAwayScoreNotNull();

    List<Match> findAllByRefereeInAndDateGreaterThanEqualAndDateLessThan(Collection<Referee> referees,
                                                                         LocalDateTime from, LocalDateTime to);

    default List<Match> findAllByRefereeInAndDateOnDay(Collection<Referee> referees, LocalDateTime dateTime) {
        var dayStart = dateTime.toLocalDate().atStartOfDay();
        return findAllByRefereeInAndDateGreaterThanEqualAndDateLessThan(referees, dayStart, dayStart.plusDays(1));
    }
}
