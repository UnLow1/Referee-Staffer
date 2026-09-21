package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.Config
import com.jamex.refereestaffer.model.entity.ConfigName
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.ConfigurationRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import spock.lang.Specification
import spock.lang.Subject

class RefereeServiceSpec extends Specification {

    @Subject
    RefereeService refereeService

    RefereeRepository refereeRepository = Mock()
    MatchRepository matchRepository = Mock()
    ConfigurationRepository configurationRepository = Mock()

    def setup() {
        refereeService = new RefereeService(refereeRepository, matchRepository, configurationRepository)
    }

    def "should keep referees of reassignable matches in the pool and drop the ones with a kept assignment"() {
        given:
        Short queue = 4
        def (autoAssigned, free) = createReferees()
        def finishedReferee = Referee.builder().id(3L).firstName("Finished").lastName("Referee").build()
        def centralReferee = Referee.builder().id(4L).firstName("S").lastName("C").build()
        def team1 = Team.builder().name("team1").build()
        def team2 = Team.builder().name("team2").build()
        def queueMatches = [
                // open match — nobody is blocked by it
                Match.builder().id(1L).home(team1).away(team2).build(),
                // previously staffed, still reassignable: its referee must stay available,
                // because a regenerate re-decides exactly this assignment
                Match.builder().id(2L).home(team2).away(team1).referee(autoAssigned).build(),
                // finished — its referee is locked into this queue for good
                Match.builder().id(3L).home(team1).away(team2).referee(finishedReferee)
                        .homeScore((short) 1).awayScore((short) 0).build(),
                // central assignment — likewise untouchable
                Match.builder().id(4L).home(team2).away(team1).referee(centralReferee).build()
        ]

        when:
        def result = refereeService.getAvailableRefereesForQueue(queue)

        then:
        1 * matchRepository.findAllByQueue(queue) >> queueMatches
        1 * refereeRepository.findAll() >> [autoAssigned, free, finishedReferee, centralReferee]
        result == [autoAssigned, free]
    }

    def "should ignore the staffed queue's own matches when calculating stats"() {
        given:
        def referee = Referee.builder().id(1L).firstName("John").lastName("Smith").build()
        def team1 = Team.builder().name("team1").build()
        def team2 = Team.builder().name("team2").build()
        def team3 = Team.builder().name("team3").build()
        // The match being re-decided must not count: neither as a refereed match nor as
        // familiarity with team1/team2, otherwise a regenerate penalises the referee for
        // the very assignment it is about to reshuffle.
        def matchBeingStaffed = Match.builder().id(10L).home(team1).away(team2).referee(referee).build()
        def otherMatch = Match.builder().id(11L).home(team1).away(team3).referee(referee).build()

        when:
        refereeService.calculateStats([referee], [10L] as Set)

        then:
        1 * matchRepository.findAllByRefereeIn([referee]) >> [matchBeingStaffed, otherMatch]
        referee.numberOfMatchesInRound == (short) 1
        referee.teamsRefereed.get(team1) == 1
        referee.teamsRefereed.get(team3) == 1
        !referee.teamsRefereed.containsKey(team2)
    }

    def "should fall back to default grade when referee has no matches"() {
        given:
        def referee = Referee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .build()

        when:
        refereeService.calculateStats([referee])

        then:
        1 * matchRepository.findAllByRefereeIn([referee]) >> []
        referee.averageGrade == RefereeService.DEFAULT_GRADE
        referee.numberOfMatchesInRound == (short) 0
    }

    def "should fall back to default grade when referee's matches have no grades yet"() {
        given:
        def referee = Referee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .build()
        def team1 = Team.builder().name("team1").build()
        def team2 = Team.builder().name("team2").build()
        def matchesWithoutGrades = [
                Match.builder().home(team1).away(team2).grade(null).referee(referee).build(),
                Match.builder().home(team2).away(team1).grade(null).referee(referee).build()
        ]

        when:
        refereeService.calculateStats([referee])

        then:
        1 * matchRepository.findAllByRefereeIn([referee]) >> matchesWithoutGrades
        referee.averageGrade == RefereeService.DEFAULT_GRADE
        referee.numberOfMatchesInRound == (short) 2
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
        def referee = referees.get(0)
        def refereesMatches = createMatches(referee, team1, team2, team3, grade1, grade2)

        when:
        refereeService.calculateStats(referees)

        then:
        1 * matchRepository.findAllByRefereeIn(referees) >> refereesMatches
        referee.averageGrade == (double) (grade1 + grade2) / 2
        referee.numberOfMatchesInRound == (short) refereesMatches.size()
        def teamRefereedMap = referee.teamsRefereed
        teamRefereedMap.size() == 3
        teamRefereedMap.get(team1) == 2
        teamRefereedMap.get(team2) == 1
        teamRefereedMap.get(team3) == 1

        and: "referees without matches in the result get the defaults"
        referees.get(1).averageGrade == RefereeService.DEFAULT_GRADE
        referees.get(1).numberOfMatchesInRound == (short) 0
    }

    def "should average split grades using their arithmetic mean"() {
        given:
        def referee = Referee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .build()
        def team1 = Team.builder().name("team1").build()
        def team2 = Team.builder().name("team2").build()
        def splitGradeMatch = Match.builder()
                .home(team1)
                .away(team2)
                .grade(Grade.builder()
                        .value(7.9d)
                        .secondValue(8.3d)
                        .build())
                .referee(referee)
                .build()
        def plainGradeMatch = Match.builder()
                .home(team2)
                .away(team1)
                .grade(Grade.builder()
                        .value(8.5d)
                        .build())
                .referee(referee)
                .build()

        when:
        refereeService.calculateStats([referee])

        then:
        1 * matchRepository.findAllByRefereeIn([referee]) >> [splitGradeMatch, plainGradeMatch]
        referee.averageGrade == ((7.9d + 8.3d) / 2 + 8.5d) / 2
    }

    def "should count home and away wins skipping draws and unfinished matches"() {
        given:
        def referee = Referee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .build()
        def team1 = Team.builder().name("team1").build()
        def team2 = Team.builder().name("team2").build()
        def matches = [
                // two home wins, one away win
                Match.builder().home(team1).away(team2).homeScore((short) 2).awayScore((short) 0).referee(referee).build(),
                Match.builder().home(team2).away(team1).homeScore((short) 3).awayScore((short) 1).referee(referee).build(),
                Match.builder().home(team1).away(team2).homeScore((short) 0).awayScore((short) 1).referee(referee).build(),
                // draw — counts for neither side
                Match.builder().home(team2).away(team1).homeScore((short) 1).awayScore((short) 1).referee(referee).build(),
                // unfinished matches — count for neither side
                Match.builder().home(team1).away(team2).referee(referee).build(),
                Match.builder().home(team2).away(team1).homeScore((short) 2).referee(referee).build()
        ]

        when:
        refereeService.calculateStats([referee])

        then:
        1 * matchRepository.findAllByRefereeIn([referee]) >> matches
        referee.homeWins == (short) 2
        referee.awayWins == (short) 1
    }

    def "should set zero win counters for referee without matches"() {
        given:
        def referee = Referee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .build()

        when:
        refereeService.calculateStats([referee])

        then:
        1 * matchRepository.findAllByRefereeIn([referee]) >> []
        referee.homeWins == (short) 0
        referee.awayWins == (short) 0
    }

    def "should enrich referees with potential computed from average grade and experience"() {
        given:
        def avgMultiplier = 6.0d
        def expMultiplier = 0.5d
        def referee = Referee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .experience(10)
                .build()
        def team1 = Team.builder().name("team1").build()
        def team2 = Team.builder().name("team2").build()
        def grade1 = 8.0
        def grade2 = 9.0
        def matches = createMatches(referee, team1, team2, team2, grade1, grade2)

        when:
        refereeService.enrichWithStats([referee])

        then:
        1 * matchRepository.findAllByRefereeIn([referee]) >> matches
        1 * configurationRepository.findByName(ConfigName.AVERAGE_GRADE_MULTIPLIER) >> new Config(ConfigName.AVERAGE_GRADE_MULTIPLIER, avgMultiplier)
        1 * configurationRepository.findByName(ConfigName.EXPERIENCE_MULTIPLIER) >> new Config(ConfigName.EXPERIENCE_MULTIPLIER, expMultiplier)

        def expectedAverage = (grade1 + grade2) / 2
        referee.averageGrade == expectedAverage
        referee.potential == avgMultiplier * expectedAverage + expMultiplier * referee.experience
    }

    def "should compute potential from default grade when referee has no grades yet"() {
        given:
        def avgMultiplier = 6.0d
        def expMultiplier = 0.5d
        def referee = Referee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .experience(4)
                .build()

        when:
        refereeService.enrichWithStats([referee])

        then:
        1 * matchRepository.findAllByRefereeIn([referee]) >> []
        1 * configurationRepository.findByName(ConfigName.AVERAGE_GRADE_MULTIPLIER) >> new Config(ConfigName.AVERAGE_GRADE_MULTIPLIER, avgMultiplier)
        1 * configurationRepository.findByName(ConfigName.EXPERIENCE_MULTIPLIER) >> new Config(ConfigName.EXPERIENCE_MULTIPLIER, expMultiplier)

        referee.averageGrade == RefereeService.DEFAULT_GRADE
        referee.potential == avgMultiplier * RefereeService.DEFAULT_GRADE + expMultiplier * referee.experience
    }

    static List<Referee> createReferees() {
        def ref1 = Referee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Smith")
                .build()
        def ref2 = Referee.builder()
                .id(2L)
                .firstName("Affan")
                .lastName("Bradshaw")
                .build()
        def refSC = Referee.builder()
                .id(3L)
                .firstName("S")
                .lastName("C")
                .build()

        return [ref1, ref2, refSC]
    }

    static List<Match> createMatches(Referee referee, Team team1, Team team2, Team team3, double grade1, double grade2) {
        def match1 = Match.builder()
                .home(team1)
                .away(team2)
                .grade(Grade.builder()
                        .value(grade1)
                        .build())
                .referee(referee)
                .build()
        def match2 = Match.builder()
                .home(team1)
                .away(team3)
                .grade(Grade.builder()
                        .value(grade2)
                        .build())
                .referee(referee)
                .build()

        return [match1, match2]
    }
}
