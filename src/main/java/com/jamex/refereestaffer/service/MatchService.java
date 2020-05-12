package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Match;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MatchService {
    private static final int POINTS_FOR_WIN_MATCH = 3;
    private static final int POINTS_FOR_DRAW_MATCH = 1;

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
}
