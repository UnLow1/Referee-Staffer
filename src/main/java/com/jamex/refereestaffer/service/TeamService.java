package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.TeamConverter;
import com.jamex.refereestaffer.model.dto.TeamDto;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final MatchService matchService;

    public Collection<Team> getStandings() {
        var matches = matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull();
        matchService.calculatePointsForTeams(matches);

        var comparatorByPoints = Comparator.comparingInt(Team::getPoints).reversed();
        var teams = matches.stream()
                .flatMap(match -> Stream.of(match.getHome(), match.getAway()))
                .distinct()
                .sorted(comparatorByPoints)
                .collect(Collectors.toList());

        var teamIds = teams.stream()
                .map(Team::getId)
                .collect(Collectors.toList());
        List<Team> teamsWithoutMatches;
        if (teamIds.isEmpty())
            teamsWithoutMatches = teamRepository.findAll();
        else
            teamsWithoutMatches = teamRepository.findAllByIdNotIn(teamIds);
        teams.addAll(teamsWithoutMatches);

        return teams;
    }
}
