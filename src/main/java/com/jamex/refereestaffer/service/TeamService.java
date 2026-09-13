package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.dto.StandingsDto;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

@Service
public class TeamService {

    /** Shared zeroed stats for teams without a finished match — never mutated. */
    private static final TeamStats NO_MATCHES = new TeamStats();

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    public TeamService(TeamRepository teamRepository, MatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    /**
     * Builds the full league table from finished matches. Every team is included —
     * teams without a finished match get zeroed stats and sort to the bottom. Points
     * use the same weights as {@link MatchService#calculatePointsForTeams} (shared
     * constants); ties break by goal difference, then goals scored, then name.
     */
    public StandingsDto getStandings() {
        var finishedMatches = matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull();

        var statsByTeamId = new HashMap<Long, TeamStats>();
        for (var match : finishedMatches) {
            statsByTeamId.computeIfAbsent(match.getHome().getId(), id -> new TeamStats())
                    .addResult(match.getHomeScore(), match.getAwayScore());
            statsByTeamId.computeIfAbsent(match.getAway().getId(), id -> new TeamStats())
                    .addResult(match.getAwayScore(), match.getHomeScore());
        }

        var sortedTeams = teamRepository.findAll().stream()
                .sorted(tableOrder(statsByTeamId))
                .toList();
        var rows = IntStream.range(0, sortedTeams.size())
                .mapToObj(i -> toRow(sortedTeams.get(i), (short) (i + 1), statsByTeamId))
                .toList();

        var afterQueue = finishedMatches.stream()
                .map(Match::getQueue)
                .max(Short::compare)
                .orElse(null);
        return new StandingsDto(afterQueue, rows);
    }

    private static Comparator<Team> tableOrder(Map<Long, TeamStats> statsByTeamId) {
        Comparator<Team> byPoints = Comparator.comparingInt(team -> statsOf(team, statsByTeamId).points());
        Comparator<Team> byGoalDifference = Comparator.comparingInt(team -> statsOf(team, statsByTeamId).goalDifference());
        Comparator<Team> byGoalsFor = Comparator.comparingInt(team -> statsOf(team, statsByTeamId).goalsFor);
        return byPoints.reversed()
                .thenComparing(byGoalDifference.reversed())
                .thenComparing(byGoalsFor.reversed())
                .thenComparing(Team::getName);
    }

    private static StandingsDto.Row toRow(Team team, short place, Map<Long, TeamStats> statsByTeamId) {
        var stats = statsOf(team, statsByTeamId);
        return new StandingsDto.Row(team.getId(), team.getName(), team.getCity(), team.getShortCode(),
                (short) stats.points(), place, (short) stats.played, (short) stats.wins, (short) stats.draws,
                (short) stats.losses, (short) stats.goalsFor, (short) stats.goalsAgainst);
    }

    private static TeamStats statsOf(Team team, Map<Long, TeamStats> statsByTeamId) {
        return statsByTeamId.getOrDefault(team.getId(), NO_MATCHES);
    }

    /** Per-team accumulator. Ints internally — narrowed to short only when building the DTO row. */
    private static final class TeamStats {
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;

        void addResult(short scored, short conceded) {
            played++;
            goalsFor += scored;
            goalsAgainst += conceded;
            if (scored > conceded)
                wins++;
            else if (scored < conceded)
                losses++;
            else
                draws++;
        }

        int points() {
            return wins * MatchService.POINTS_FOR_WIN_MATCH + draws * MatchService.POINTS_FOR_DRAW_MATCH;
        }

        int goalDifference() {
            return goalsFor - goalsAgainst;
        }
    }
}
