package com.jamex.refereestaffer.mcp

import com.jamex.refereestaffer.model.dto.DifficultyBreakdownDto
import com.jamex.refereestaffer.model.dto.StandingsDto
import com.jamex.refereestaffer.model.entity.Config
import com.jamex.refereestaffer.model.entity.ConfigName
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.model.entity.Vacation
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException
import com.jamex.refereestaffer.repository.ConfigurationRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.VacationRepository
import com.jamex.refereestaffer.service.MatchService
import com.jamex.refereestaffer.service.RefereeService
import com.jamex.refereestaffer.service.TeamService
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit spec for the transport-agnostic half of the MCP server. Everything the tools
 * answer with is shaped here, so this is where the shaping is pinned down — the
 * {@code @McpTool} layer on top is pure delegation and has its own (much shorter) spec.
 */
class StaffingInsightsSpec extends Specification {

    @Subject
    StaffingInsights insights

    RefereeRepository refereeRepository = Mock()
    MatchRepository matchRepository = Mock()
    VacationRepository vacationRepository = Mock()
    ConfigurationRepository configurationRepository = Mock()
    RefereeService refereeService = Mock()
    MatchService matchService = Mock()
    TeamService teamService = Mock()

    def setup() {
        insights = new StaffingInsights(refereeRepository, matchRepository, vacationRepository,
                configurationRepository, refereeService, matchService, teamService)
    }

    def "should list referees with stats, joining the name and flagging the central sentinel"() {
        given:
        def john = referee(1L, "John", "Smith")
        john.averageGrade = 8.4d
        john.potential = 420.0d
        john.numberOfMatchesInRound = (short) 7
        john.lastQueue = (short) 3
        def central = referee(2L, "S", "C")

        when:
        def result = insights.listReferees()

        then:
        1 * refereeRepository.findAll() >> [john, central]
        1 * refereeService.enrichWithStats([john, central])

        and:
        result.size() == 2
        result[0].name == "John Smith"
        result[0].averageGrade == 8.4d
        result[0].potential == 420.0d
        result[0].matchesRefereed == (short) 7
        result[0].lastQueue == (short) 3
        !result[0].central

        and: "the 'S C' sentinel is marked so a model does not propose reassigning it"
        result[1].name == "S C"
        result[1].central
    }

    def "should build a referee profile with match history, grades and vacations"() {
        given:
        def john = referee(1L, "John", "Smith")
        john.homeWins = (short) 2
        john.awayWins = (short) 1
        def splitGrade = Grade.builder().value(7.9d).secondValue(8.3d).build()
        def played = match(10L, (short) 1, LocalDateTime.of(2026, 3, 1, 12, 0),
                new Team("Legia", "Warszawa"), new Team("Cracovia", "Kraków"), (short) 0, (short) 2, splitGrade)
        def upcoming = match(11L, (short) 2, LocalDateTime.of(2026, 3, 8, 12, 0),
                new Team("Lech", "Poznań"), new Team("Wisła", "Kraków"), null, null, null)

        when:
        def profile = insights.getRefereeProfile(1L)

        then:
        1 * refereeRepository.findById(1L) >> Optional.of(john)
        1 * refereeService.enrichWithStats([john])
        // Deliberately returned out of order — the profile sorts by date itself.
        1 * matchRepository.findAllByRefereeIn([john]) >> [upcoming, played]
        1 * vacationRepository.findAllByRefereeIdOrderByStartDateAsc(1L) >> [
                new Vacation(5L, john, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14))
        ]

        and:
        profile.referee().name == "John Smith"
        profile.homeWins() == (short) 2
        profile.awayWins() == (short) 1

        and: "matches come back oldest first"
        profile.matches()*.matchId() == [10L, 11L]

        and: "a split grade reports both the mean and the original notation"
        profile.matches()[0].score() == "0:2"
        profile.matches()[0].grade() == (7.9d + 8.3d) / 2
        profile.matches()[0].gradeAsAwarded() == "7.9/8.3"

        and: "an unplayed, ungraded match reports nulls rather than zeros"
        profile.matches()[1].score() == null
        profile.matches()[1].grade() == null
        profile.matches()[1].gradeAsAwarded() == null

        and:
        profile.vacations()*.startDate() == [LocalDate.of(2026, 7, 1)]
    }

    def "should reject an unknown referee id instead of returning an empty profile"() {
        when:
        insights.getRefereeProfile(404L)

        then:
        1 * refereeRepository.findById(404L) >> Optional.empty()
        thrown(RefereeNotFoundException)
    }

    def "should page matches filtered by queue"() {
        given:
        def john = referee(1L, "John", "Smith")
        def assigned = match(10L, (short) 1, LocalDateTime.of(2026, 3, 1, 12, 0),
                new Team("Legia", "Warszawa"), new Team("Cracovia", "Kraków"), (short) 1, (short) 1, null)
        assigned.referee = john
        def unassigned = match(11L, (short) 1, LocalDateTime.of(2026, 3, 1, 14, 30),
                new Team("Lech", "Poznań"), new Team("Wisła", "Kraków"), null, null, null)
        Short queue = 1

        when:
        def page = insights.listMatches(queue, 1, 2)

        then:
        1 * matchRepository.findAllByQueue(queue, _ as Pageable) >> { Short q, Pageable pageable ->
            assert pageable.pageNumber == 1
            assert pageable.pageSize == 2
            assert pageable.sort.collect { it.property } == ["queue", "date"]
            new PageImpl<Match>([assigned, unassigned], pageable, 6)
        }
        0 * matchRepository.findAll(_ as Pageable)

        and:
        page.queue() == queue
        page.page() == 1
        page.pageSize() == 2
        page.totalMatches() == 6
        page.totalPages() == 3

        and:
        page.matches()[0].refereeId() == 1L
        page.matches()[0].refereeName() == "John Smith"
        page.matches()[0].homeTeam() == "Legia"
        page.matches()[0].homeScore() == (short) 1

        and: "a match still waiting for a cast reports no referee at all"
        page.matches()[1].refereeId() == null
        page.matches()[1].refereeName() == null
        !page.matches()[1].central()
    }

    def "should list matches across all queues when no queue is given"() {
        when:
        def page = insights.listMatches(null, null, null)

        then:
        1 * matchRepository.findAll(_ as Pageable) >> { Pageable pageable ->
            assert pageable.pageSize == StaffingInsights.DEFAULT_PAGE_SIZE
            new PageImpl<Match>([], pageable, 0)
        }
        0 * matchRepository.findAllByQueue(_, _)

        and:
        page.queue() == null
        page.page() == 0
        page.pageSize() == StaffingInsights.DEFAULT_PAGE_SIZE
    }

    def "should clamp out-of-range paging instead of failing the tool call"() {
        when:
        def page = insights.listMatches(null, requestedPage, requestedSize)

        then:
        1 * matchRepository.findAll(_ as Pageable) >> { Pageable pageable -> new PageImpl<Match>([], pageable, 0) }
        page.page() == expectedPage
        page.pageSize() == expectedSize

        where:
        requestedPage | requestedSize || expectedPage | expectedSize
        -5            | 10            || 0            | 10
        0             | 0             || 0            | 1
        0             | 5_000         || 0            | StaffingInsights.MAX_PAGE_SIZE
    }

    def "should delegate difficulty and standings to the services that own the formulas"() {
        given:
        def breakdown = new DifficultyBreakdownDto(10L, 104.0d,
                new DifficultyBreakdownDto.Parts(97.0d, 0.0d, 7.0d, 0.0d),
                new DifficultyBreakdownDto.Flags(false, true, false, 3))
        def standings = new StandingsDto((short) 3, [])

        when:
        def difficultyResult = insights.explainMatchDifficulty(10L)
        def standingsResult = insights.getStandings()

        then:
        1 * matchService.computeDifficultyBreakdown(10L) >> breakdown
        1 * teamService.getStandings() >> standings

        and: "both DTOs are already minimal, so they are passed through untouched"
        difficultyResult.is(breakdown)
        standingsResult.is(standings)
    }

    def "should expose algorithm config in ConfigName order with group and description"() {
        when:
        def parameters = insights.getAlgorithmConfig()

        then:
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.EXPERIENCE_MULTIPLIER)        : 0.1d,
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER)  : 1.0d,
                (ConfigName.AVERAGE_GRADE_MULTIPLIER)     : 50.0d,
        ]

        and: "enum declaration order, so the difficulty/potential groups stay contiguous"
        parameters*.name() == ["DIFFICULTY_LEVEL_MULTIPLIER", "AVERAGE_GRADE_MULTIPLIER", "EXPERIENCE_MULTIPLIER"]
        parameters*.group() == ["difficulty", "potential", "potential"]
        parameters[0].value() == 1.0d
        parameters[0].description() == ConfigName.DIFFICULTY_LEVEL_MULTIPLIER.description()
    }

    def "should skip config keys that have no row yet rather than emitting a null value"() {
        when:
        def parameters = insights.getAlgorithmConfig()

        then:
        1 * configurationRepository.findAllAsMap() >> [(ConfigName.NUMBER_OF_EDGE_TEAMS): 3.0d]
        parameters*.name() == ["NUMBER_OF_EDGE_TEAMS"]
    }

    def "should describe the algorithm naming every configurable weight"() {
        when:
        def text = insights.describeAlgorithm()

        then:
        0 * _

        and: "the explainer has to stay in step with the config keys, or it starts lying"
        ConfigName.values().every { text.contains(it.name()) }
        text.contains("Referee potential")
        text.contains("Match difficulty")
        text.contains("Effective value")
    }

    def "should render the configuration resource grouped as plain text"() {
        when:
        def text = insights.describeConfiguration()

        then:
        1 * configurationRepository.findAllAsMap() >> [
                (ConfigName.DIFFICULTY_LEVEL_MULTIPLIER): 1.0d,
                (ConfigName.AVERAGE_GRADE_MULTIPLIER)   : 50.0d,
        ]

        and:
        text.contains("[difficulty]")
        text.contains("[potential]")
        text.contains("DIFFICULTY_LEVEL_MULTIPLIER = 1.0")
        text.contains("AVERAGE_GRADE_MULTIPLIER = 50.0")
        text.contains(ConfigName.AVERAGE_GRADE_MULTIPLIER.description())
    }

    private static Referee referee(Long id, String firstName, String lastName) {
        Referee.builder().id(id).firstName(firstName).lastName(lastName).email("x@y.z").experience(5).build()
    }

    private static Match match(Long id, Short queue, LocalDateTime date, Team home, Team away,
                               Short homeScore, Short awayScore, Grade grade) {
        // Match has no grade setter (the association is owned by Grade), so the all-args
        // constructor is the only way to hand a spec a graded match.
        new Match(id, queue, home, away, date, null, grade, homeScore, awayScore, 0.0d)
    }
}
