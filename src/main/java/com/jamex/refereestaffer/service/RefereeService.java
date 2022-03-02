package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefereeService {

    private final RefereeRepository refereeRepository;
    private final MatchRepository matchRepository;

    public List<Referee> getAvailableRefereesForQueue(Short queue) {
        var referees = refereeRepository.findAllWithNoMatchInQueue(queue).stream()
                // filtering out "SC" referees is only for test data which I have
                .filter(referee -> !referee.getFirstName().equals("S"))
                .filter(referee -> !referee.getLastName().equals("C"))
                .toList();
        for (var referee : referees) {
            var matchesForReferee = matchRepository.findAllByReferee(referee);

            var averageGrade = countAverageGrade(matchesForReferee);
            var teamsRefereedMap = createTeamsRefereedMap(matchesForReferee);

            referee.setAverageGrade(averageGrade);
            referee.setTeamsRefereed(teamsRefereedMap);
            referee.setNumberOfMatchesInRound((short) matchesForReferee.size());
        }
        return referees;
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
