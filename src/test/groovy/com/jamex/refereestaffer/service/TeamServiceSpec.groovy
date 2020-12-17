package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.TeamRepository
import spock.lang.Specification
import spock.lang.Subject

class TeamServiceSpec extends Specification {

    @Subject
    TeamService teamService

    TeamRepository teamRepository = Mock()
    MatchRepository matchRepository = Mock()
    MatchService matchService = Mock()

    def setup() {
        teamService = new TeamService(teamRepository, matchRepository, matchService)
    }

    def "should return standings (some matches has been played)"() {
        given:
        def team1 = [points: 6] as Team
        def team2 = [points: 10] as Team
        def team3 = [points: 3] as Team
        def team4 = [points: 9] as Team
        def team5 = [getId: 123] as Team
        def matches = [[getHome: team1, getAway: team2] as Match, [getHome: team3, getAway: team4] as Match]

        when:
        def result = teamService.getStandings()

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> matches
        1 * matchService.calculatePointsForTeams(matches)
        1 * teamRepository.findAllByIdNotIn(_) >> [team5]
        result.size() == 5
        result == [team2, team4, team1, team3, team5]
    }

    def "should return standings (no matches have been played)"() {
        given:
        def teams = [[] as Team, [] as Team]

        when:
        def result = teamService.getStandings()

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        1 * matchService.calculatePointsForTeams([])
        1 * teamRepository.findAll() >> teams
        result == teams
    }
}
