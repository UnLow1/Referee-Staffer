package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findAllByRefereeIn(Collection<Referee> referees);

    List<Match> findAllByQueue(Short queue);

    List<Match> findAllByQueueOrderByDateAsc(Short queue);

    /** Paged variant for the MCP match listing, where an unbounded result would flood a model's context. */
    Page<Match> findAllByQueue(Short queue, Pageable pageable);

    List<Match> findAllByHomeScoreNotNullAndAwayScoreNotNull();

    List<Match> findAllByRefereeInAndDateGreaterThanEqualAndDateLessThan(Collection<Referee> referees,
                                                                         LocalDateTime from, LocalDateTime to);

    default List<Match> findAllByRefereeInAndDateOnDay(Collection<Referee> referees, LocalDateTime dateTime) {
        var dayStart = dateTime.toLocalDate().atStartOfDay();
        return findAllByRefereeInAndDateGreaterThanEqualAndDateLessThan(referees, dayStart, dayStart.plusDays(1));
    }
}
