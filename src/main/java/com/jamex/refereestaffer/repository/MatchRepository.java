package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
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

    long countByReferee(Referee referee);

    // Spelled out rather than derived: countByHomeOrAway(a, b) would also compile and
    // answer a different question than "how many matches does this team play".
    @Query("select count(m) from Match m where m.home = :team or m.away = :team")
    long countByTeam(@Param("team") Team team);

    List<Match> findAllByQueue(Short queue);

    List<Match> findAllByQueueOrderByDateAsc(Short queue);

    List<Match> findAllByHomeScoreNotNullAndAwayScoreNotNull();

    List<Match> findAllByRefereeInAndDateGreaterThanEqualAndDateLessThan(Collection<Referee> referees,
                                                                         LocalDateTime from, LocalDateTime to);

    default List<Match> findAllByRefereeInAndDateOnDay(Collection<Referee> referees, LocalDateTime dateTime) {
        var dayStart = dateTime.toLocalDate().atStartOfDay();
        return findAllByRefereeInAndDateGreaterThanEqualAndDateLessThan(referees, dayStart, dayStart.plusDays(1));
    }
}
