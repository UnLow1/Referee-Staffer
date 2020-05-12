package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.TeamConverter;
import com.jamex.refereestaffer.model.dto.TeamDto;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class TeamService {

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final TeamConverter teamConverter;

    public Collection<TeamDto> getStandings() {
        var matches = matchRepository.findAll();
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
        var teamsWithoutMatches = teamRepository.findAllByIdNotIn(teamIds);
        teams.addAll(teamsWithoutMatches);

        return teamConverter.convertFromEntities(teams);
    }
}
