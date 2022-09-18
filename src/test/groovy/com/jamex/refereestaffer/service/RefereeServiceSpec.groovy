package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import spock.lang.Specification
import spock.lang.Subject

class RefereeServiceSpec extends Specification {

    @Subject
    RefereeService refereeService

    RefereeRepository refereeRepository = Mock()
    MatchRepository matchRepository = Mock()

    def "setup"() {
        refereeService = new RefereeService(refereeRepository, matchRepository)
    }

    def "should return available referees"() {
        given:
        Short queue = 4
        def availableReferees = createReferees()

        when:
        def result = refereeService.getAvailableRefereesForQueue(queue)

        then:
        1 * refereeRepository.findAllWithNoMatchInQueue(queue) >> availableReferees
        result.size() == 2
    }

    def "should calculate referees stats"() {
        given:
        def referees = createReferees()
        def grade1 = 8.3
        def grade2 = 8.1
        def team1 = Team.builder()
                .name("team1")
                .build()
        def team2 = Team.builder()
                .name("team2")
                .build()
        def team3 = Team.builder()
                .name("team3")
                .build()
        def refereesMatches = createMatches(team1, team2, team3, grade1, grade2)

        when:
        refereeService.calculateStats(referees)

        then:
        3 * matchRepository.findAllByReferee(_) >> refereesMatches
        def referee = referees.get(0)
        referee.averageGrade == (double) (grade1 + grade2) / 2
        referee.numberOfMatchesInRound == (short) refereesMatches.size()
        def teamRefereedMap = referee.teamsRefereed
        teamRefereedMap.size() == 3
        teamRefereedMap.get(team1) == 2
        teamRefereedMap.get(team2) == 1
        teamRefereedMap.get(team3) == 1
    }

    static List<Referee> createReferees() {
        def ref1 = Referee.builder()
                .firstName("John")
                .lastName("Smith")
                .build()
        def ref2 = Referee.builder()
                .firstName("Affan")
                .lastName("Bradshaw")
                .build()
        def refSC = Referee.builder()
                .firstName("S")
                .lastName("C")
                .build()

        return [ref1, ref2, refSC]
    }

    static List<Match> createMatches(Team team1, Team team2, Team team3, double grade1, double grade2) {
        def match1 = Match.builder()
                .home(team1)
                .away(team2)
                .grade(Grade.builder()
                        .value(grade1)
                        .build())
                .build()
        def match2 = Match.builder()
                .home(team1)
                .away(team3)
                .grade(Grade.builder()
                        .value(grade2)
                        .build())
                .build()

        return [match1, match2]
    }
}
