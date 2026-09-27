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

    List<Match> findAllByQueueOrderByDateAsc(Short queue);

    List<Match> findAllByHomeScoreNotNullAndAwayScoreNotNull();

    /**
     * Everything the staffing rules can refer to for a set of referees, in one query: matches
     * inside the day range (same-day conflicts, which span queues because a match can be
     * rescheduled onto the day) union matches in the given queues (double-booking inside a
     * queue, which spans days for the same reason). One query rather than two, because for a
     * pool built from {@code findAllWithNoMatchInQueue} the queue half is empty by
     * construction and a separate lookup for it would be a guaranteed-empty round trip.
     */
    @Query("SELECT m FROM Match m WHERE m.referee IN :referees " +
            "AND ((m.date >= :from AND m.date < :to) OR m.queue IN :queues)")
    List<Match> findAllByRefereeInOnDaysOrInQueues(@Param("referees") Collection<Referee> referees,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to,
                                                   @Param("queues") Collection<Short> queues);
}
