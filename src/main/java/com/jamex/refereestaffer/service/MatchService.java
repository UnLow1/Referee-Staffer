package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.exception.MatchNotFoundException;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MatchService {

    private static final int POINTS_FOR_WIN_MATCH = 3;
    private static final int POINTS_FOR_DRAW_MATCH = 1;

    private final MatchRepository matchRepository;
    private final GradeRepository gradeRepository;

    public void calculatePointsForTeams(List<Match> matches) {
        for (var match : matches) {
            if (match.getHomeScore() > match.getAwayScore())
                match.getHome().addPoints(POINTS_FOR_WIN_MATCH);
            else if (match.getHomeScore() < match.getAwayScore())
                match.getAway().addPoints(POINTS_FOR_WIN_MATCH);
            else {
                match.getHome().addPoints(POINTS_FOR_DRAW_MATCH);
                match.getAway().addPoints(POINTS_FOR_DRAW_MATCH);
            }
        }
    }

    public void deleteMatch(Long matchId) {
        var match = matchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        log.info("Deleting grade with id = " + match.getGrade().getId());
        gradeRepository.delete(match.getGrade());
        matchRepository.delete(match);
    }
}
