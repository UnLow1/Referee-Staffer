package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.TeamRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class TeamServiceSpec extends Specification {

    @Subject
    TeamService teamService

    TeamRepository teamRepository = Mock()
    MatchRepository matchRepository = Mock()

    def setup() {
        teamService = new TeamService(teamRepository, matchRepository)
    }

    def "should compute the full table from finished matches"() {
        given:
        def alfa = Team.builder().id(1l).name("Alfa").build()
        def beta = Team.builder().id(2l).name("Beta").build()
        def gamma = Team.builder().id(3l).name("Gamma").build()
        def delta = Team.builder().id(4l).name("Delta").build()
        def matches = [finishedMatch(1, alfa, beta, 3, 1),
                       finishedMatch(2, alfa, gamma, 2, 2)]

        when:
        def result = teamService.getStandings()

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> matches
        1 * teamRepository.findAll() >> [alfa, beta, gamma, delta]

        and: "afterQueue is the highest queue with a finished match"
        result.afterQueue() == 2 as Short

        and: "teams are ranked by points, zero-point teams tie-break by goal difference"
        result.rows()*.name() == ["Alfa", "Gamma", "Delta", "Beta"]
        result.rows()*.place() == (1..4).collect { it as Short }

        and: "winner's stats add up across both matches"
        with(result.rows()[0]) {
            points() == 4 as Short
            played() == 2 as Short
            wins() == 1 as Short
            draws() == 1 as Short
            losses() == 0 as Short
            goalsFor() == 5 as Short
            goalsAgainst() == 3 as Short
        }

        and: "loser gets zero points and mirrored goals"
        with(result.rows()[3]) {
            points() == 0 as Short
            played() == 1 as Short
            losses() == 1 as Short
            goalsFor() == 1 as Short
            goalsAgainst() == 3 as Short
        }

        and: "a team without finished matches gets zeroed stats"
        with(result.rows()[2]) {
            name() == "Delta"
            points() == 0 as Short
            played() == 0 as Short
        }
    }

    def "should break point ties by goal difference, then goals scored, then name"() {
        given: "four teams with one win each (3 points)"
        def alfa = Team.builder().id(1l).name("Alfa").build()
        def beta = Team.builder().id(2l).name("Beta").build()
        def gamma = Team.builder().id(3l).name("Gamma").build()
        def delta = Team.builder().id(4l).name("Delta").build()
        def other = Team.builder().id(5l).name("Omega").build()
        def matches = [finishedMatch(1, alfa, other, 1, 0), // GD +1, GF 1
                       finishedMatch(1, beta, other, 3, 1), // GD +2, GF 3
                       finishedMatch(1, gamma, other, 2, 1), // GD +1, GF 2
                       finishedMatch(1, delta, other, 1, 0)] // GD +1, GF 1 — full tie with Alfa

        when:
        def result = teamService.getStandings()

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> matches
        1 * teamRepository.findAll() >> [alfa, beta, gamma, delta, other]
        result.rows()*.name() == ["Beta", "Gamma", "Alfa", "Delta", "Omega"]
    }

    def "should return an alphabetical zero-stats table before the season starts"() {
        given:
        def wisla = Team.builder().id(1l).name("Wisla").build()
        def legia = Team.builder().id(2l).name("Legia").build()

        when:
        def result = teamService.getStandings()

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        1 * teamRepository.findAll() >> [wisla, legia]
        result.afterQueue() == null
        result.rows()*.name() == ["Legia", "Wisla"]
        result.rows()*.place() == [1 as Short, 2 as Short]
        result.rows().every { it.points() == 0 as Short && it.played() == 0 as Short }
    }

    def "should return an empty table when there are no teams"() {
        when:
        def result = teamService.getStandings()

        then:
        1 * matchRepository.findAllByHomeScoreNotNullAndAwayScoreNotNull() >> []
        1 * teamRepository.findAll() >> []
        result.afterQueue() == null
        result.rows().isEmpty()
    }

    private static Match finishedMatch(int queue, Team home, Team away, int homeScore, int awayScore) {
        new Match(queue as short, home, away, LocalDateTime.now(), null, homeScore as Short, awayScore as Short)
    }
}
