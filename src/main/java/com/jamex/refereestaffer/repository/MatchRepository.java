package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findAllByQueue(Short queue);

    List<Match> findAllByReferee(Referee referee);
}
