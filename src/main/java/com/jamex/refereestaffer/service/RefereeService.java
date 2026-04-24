package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RefereeService {

    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;

    public RefereeService(RefereeRepository refereeRepository, MatchRepository matchRepository) {
        this.refereeRepository = refereeRepository;
        this.matchRepository = matchRepository;
    }

    public List<Referee> getAvailableRefereesForQueue(Short queue) {
        return refereeRepository.findAllWithNoMatchInQueue(queue).stream()
                // "S C" = "Sędzia z Centrali" (PZPN central-level referee assigned top-down).
                // Such matches already have a referee set in the imported data and must not be
                // reassigned by the staffer. The CSV stores these assignments with firstName="S",
                // lastName="C" as a sentinel, so we filter them out of the available pool.
                // TODO longer-term: model this as a Referee flag / separate column instead of a name sentinel.
                .filter(referee -> !(referee.getFirstName().equals("S") && referee.getLastName().equals("C")))
                .toList();
    }

    public void calculateStats(List<Referee> referees) {
        for (var referee : referees) {
            var matchesForReferee = matchRepository.findAllByReferee(referee);

            var averageGrade = countAverageGrade(matchesForReferee);
            var teamsRefereedMap = createTeamsRefereedMap(matchesForReferee);

            referee.setAverageGrade(averageGrade);
            referee.setTeamsRefereed(teamsRefereedMap);
            referee.setNumberOfMatchesInRound((short) matchesForReferee.size());
        }
    }

    private double countAverageGrade(List<Match> matchesForReferee) {
        var matchesWithGrade = matchesForReferee.stream()
                .map(Match::getGrade)
                .filter(Objects::nonNull)
                .toList();
        var refereeGrades = matchesWithGrade.stream()
                .map(Grade::getValue)
                .reduce(0.0, Double::sum);
        return refereeGrades / matchesWithGrade.size();
    }

    private Map<Team, Short> createTeamsRefereedMap(List<Match> matchesForReferee) {
        var result = new HashMap<Team, Short>();

        for (var match : matchesForReferee) {
            result.merge(match.getHome(), (short) 1, (a, b) -> (short) (a + b));
            result.merge(match.getAway(), (short) 1, (a, b) -> (short) (a + b));
        }

        return result;
    }
}
