package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Vacation
import com.jamex.refereestaffer.model.staffing.StaffingRule
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.VacationRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDate
import java.time.LocalDateTime

class StaffingRuleCheckerSpec extends Specification {

    static final short QUEUE = 12
    static final LocalDateTime MATCH_DATE = LocalDateTime.of(2026, 5, 9, 15, 0)
    static final LocalDate MATCH_DAY = MATCH_DATE.toLocalDate()

    @Subject
    StaffingRuleChecker checker

    MatchRepository matchRepository = Mock()
    VacationRepository vacationRepository = Mock()

    def setup() {
        checker = new StaffingRuleChecker(matchRepository, vacationRepository)
    }

    static Referee referee(long id, String firstName = "Jan", String lastName = "Kowalski") {
        Referee.builder().id(id).firstName(firstName).lastName(lastName).build()
    }

    static Match match(long id, Referee referee = null, LocalDateTime date = MATCH_DATE, short queue = QUEUE) {
        Match.builder().id(id).queue(queue).date(date).referee(referee).build()
    }

    def "should report no violations for a referee with a clear day"() {
        given:
        def ref = referee(1L)
        def theMatch = match(100L)

        when:
        def rules = checker.rulesFor([theMatch], [ref])

        then:
        rules.check(theMatch, ref) == []
        rules.isClear(theMatch, ref)
        1 * matchRepository.findAllByRefereeInOnDaysOrInQueues([ref], MATCH_DAY.atStartOfDay(), MATCH_DAY.plusDays(1).atStartOfDay(), [QUEUE] as Set) >> []
        1 * vacationRepository.findAllOverlapping(MATCH_DAY, MATCH_DAY) >> []
    }

    def "should report a vacation covering the match day"() {
        given:
        def ref = referee(1L, "Anna", "Nowak")
        def theMatch = match(100L)
        def vacation = Vacation.builder().referee(ref).startDate(start).endDate(end).build()

        when:
        def rules = checker.rulesFor([theMatch], [ref])
        def violations = rules.check(theMatch, ref)

        then:
        violations*.rule == [StaffingRule.VACATION]
        violations[0].message == String.format(StaffingRules.VACATION_MESSAGE, "Anna Nowak", start, end)
        !rules.isClear(theMatch, ref)
        1 * vacationRepository.findAllOverlapping(MATCH_DAY, MATCH_DAY) >> [vacation]
        1 * matchRepository.findAllByRefereeInOnDaysOrInQueues(_, _, _, _) >> []

        where:
        // Boundaries are inclusive on both ends.
        start                    | end
        MATCH_DAY                | MATCH_DAY
        MATCH_DAY.minusDays(3)   | MATCH_DAY
        MATCH_DAY                | MATCH_DAY.plusDays(3)
        MATCH_DAY.minusDays(3)   | MATCH_DAY.plusDays(3)
    }

    def "should not report a vacation that ends before or starts after the match day"() {
        given:
        def ref = referee(1L)
        def theMatch = match(100L)
        def vacation = Vacation.builder().referee(ref).startDate(start).endDate(end).build()

        when:
        def rules = checker.rulesFor([theMatch], [ref])

        then:
        rules.check(theMatch, ref) == []
        1 * vacationRepository.findAllOverlapping(MATCH_DAY, MATCH_DAY) >> [vacation]
        1 * matchRepository.findAllByRefereeInOnDaysOrInQueues(_, _, _, _) >> []

        where:
        start                  | end
        MATCH_DAY.minusDays(5) | MATCH_DAY.minusDays(1)
        MATCH_DAY.plusDays(1)  | MATCH_DAY.plusDays(5)
    }

    def "should ignore a vacation belonging to another referee"() {
        given:
        def ref = referee(1L)
        def otherRef = referee(2L)
        def theMatch = match(100L)
        def vacation = Vacation.builder().referee(otherRef).startDate(MATCH_DAY).endDate(MATCH_DAY).build()

        when:
        def rules = checker.rulesFor([theMatch], [ref, otherRef])

        then:
        rules.check(theMatch, ref) == []
        rules.check(theMatch, otherRef)*.rule == [StaffingRule.VACATION]
        1 * vacationRepository.findAllOverlapping(MATCH_DAY, MATCH_DAY) >> [vacation]
        1 * matchRepository.findAllByRefereeInOnDaysOrInQueues(_, _, _, _) >> []
    }

    def "should report another match on the same day from a different queue"() {
        given:
        def ref = referee(1L, "Piotr", "Zielinski")
        def theMatch = match(100L)
        // Rescheduled onto the same day from an earlier queue — invisible to the queue-level
        // availability query the candidate pool is built from (RS-57).
        def conflicting = match(200L, ref, MATCH_DAY.atTime(11, 30), (short) 9)

        when:
        def rules = checker.rulesFor([theMatch], [ref])
        def violations = rules.check(theMatch, ref)

        then:
        violations*.rule == [StaffingRule.SAME_DAY_MATCH]
        violations[0].message == String.format(StaffingRules.SAME_DAY_MATCH_MESSAGE, "Piotr Zielinski", MATCH_DAY, "11:30", 9 as short)
        1 * matchRepository.findAllByRefereeInOnDaysOrInQueues([ref], MATCH_DAY.atStartOfDay(), MATCH_DAY.plusDays(1).atStartOfDay(), [QUEUE] as Set) >> [conflicting]
        1 * vacationRepository.findAllOverlapping(_, _) >> []
    }

    def "should not report the checked match against itself"() {
        given:
        def ref = referee(1L)
        // The stored assignment: asking "what does this match break?" must not count the
        // match's own row as a same-day / same-queue conflict.
        def theMatch = match(100L, ref)

        when:
        def rules = checker.rulesFor([theMatch], [ref])

        then:
        rules.check(theMatch, ref) == []
        rules.check(theMatch) == []
        1 * matchRepository.findAllByRefereeInOnDaysOrInQueues(_, _, _, _) >> [match(100L, ref)]
        1 * vacationRepository.findAllOverlapping(_, _) >> []
    }

    def "should report a second match in the same queue on another day"() {
        given:
        def ref = referee(1L, "Marek", "Wojcik")
        def theMatch = match(100L)
        def sameQueueOtherDay = match(200L, ref, MATCH_DATE.plusDays(1), QUEUE)

        when:
        def rules = checker.rulesFor([theMatch], [ref])
        def violations = rules.check(theMatch, ref)

        then:
        violations*.rule == [StaffingRule.DOUBLE_MATCH_IN_QUEUE]
        violations[0].message == String.format(StaffingRules.DOUBLE_MATCH_IN_QUEUE_MESSAGE, "Marek Wojcik", QUEUE)
        1 * matchRepository.findAllByRefereeInOnDaysOrInQueues([ref], MATCH_DAY.atStartOfDay(), MATCH_DAY.plusDays(1).atStartOfDay(), [QUEUE] as Set) >> [sameQueueOtherDay]
        1 * vacationRepository.findAllOverlapping(_, _) >> []
    }

    def "should report every broken rule independently"() {
        given:
        def ref = referee(1L)
        def theMatch = match(100L)
        // One conflicting match, same day AND same queue — trips both match rules; the
        // vacation adds a third. Rules never suppress one another.
        def conflicting = match(200L, ref, MATCH_DATE.minusHours(4), QUEUE)
        def vacation = Vacation.builder().referee(ref).startDate(MATCH_DAY).endDate(MATCH_DAY).build()

        when:
        def rules = checker.rulesFor([theMatch], [ref])

        then:
        rules.check(theMatch, ref)*.rule == [StaffingRule.VACATION, StaffingRule.SAME_DAY_MATCH, StaffingRule.DOUBLE_MATCH_IN_QUEUE]
        1 * matchRepository.findAllByRefereeInOnDaysOrInQueues(_, _, _, _) >> [conflicting]
        1 * vacationRepository.findAllOverlapping(_, _) >> [vacation]
    }

    def "should load rule data in a fixed number of queries regardless of the matrix size"() {
        given:
        def referees = (1L..5L).collect { referee(it) }
        def matches = (1L..8L).collect { match(100L + it, null, MATCH_DATE.plusDays(it % 2)) }
        def firstDay = MATCH_DAY
        def lastDay = MATCH_DAY.plusDays(1)

        when:
        def rules = checker.rulesFor(matches, referees)
        matches.each { m -> referees.each { r -> rules.check(m, r) } }

        then:
        // 40 pairs, still one query per rule source — the day range spans every match day.
        1 * matchRepository.findAllByRefereeInOnDaysOrInQueues(referees, firstDay.atStartOfDay(), lastDay.plusDays(1).atStartOfDay(), [QUEUE] as Set) >> []
        1 * vacationRepository.findAllOverlapping(firstDay, lastDay) >> []
        0 * _
    }

    def "should reject building a snapshot from incomplete input"() {
        when:
        checker.rulesFor(matches, referees)

        then:
        // rulesFor and check must agree on what a valid snapshot is: dropping the bad element
        // here would only resurface later, as check() refusing a pair the caller thinks it asked for.
        0 * _
        def exception = thrown(IllegalArgumentException)
        exception.message == expectedMessage

        where:
        matches                                            | referees                        | expectedMessage
        [Match.builder().id(5L).date(MATCH_DATE).build()]   | [referee(1L)]                   | String.format(StaffingRuleChecker.MATCH_NOT_RULEABLE, 5L)
        [Match.builder().id(5L).queue(QUEUE).build()]       | [referee(1L)]                   | String.format(StaffingRuleChecker.MATCH_NOT_RULEABLE, 5L)
        [match(100L)]                                      | [Referee.builder().build()]     | StaffingRuleChecker.REFEREE_NOT_IDENTIFIED
    }

    def "should query nothing for an empty matrix"() {
        when:
        def rules = checker.rulesFor(matches, referees)

        then:
        // An empty IN list is invalid SQL, so the checker must not reach the repositories.
        0 * _
        rules != null

        where:
        matches                     | referees
        []                          | [referee(1L)]
        [match(100L)]               | []
        []                          | []
    }

    def "should treat an unassigned match as clean in the single-argument overload"() {
        given:
        def ref = referee(1L)
        def theMatch = match(100L)

        when:
        def rules = checker.rulesFor([theMatch], [ref])

        then:
        rules.check(theMatch) == []
        // No referee on the match means no pair to judge — not a violation, and no lookup.
        1 * matchRepository.findAllByRefereeInOnDaysOrInQueues(_, _, _, _) >> []
        1 * vacationRepository.findAllOverlapping(_, _) >> []
    }

    def "should reject a pair the snapshot was not built for"() {
        given:
        def ref = referee(1L)
        def theMatch = match(100L)
        matchRepository.findAllByRefereeInOnDaysOrInQueues(_, _, _, _) >> []
        vacationRepository.findAllOverlapping(_, _) >> []
        def rules = checker.rulesFor([theMatch], [ref])

        when:
        rules.check(outsideMatch, outsideReferee)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == expectedMessage

        where:
        outsideMatch                                      | outsideReferee            | expectedMessage
        match(100L, null, MATCH_DATE.plusDays(4))          | referee(1L)               | String.format(StaffingRules.MATCH_DAY_NOT_IN_SNAPSHOT, MATCH_DAY.plusDays(4))
        match(100L, null, MATCH_DATE, (short) 99)          | referee(1L)               | String.format(StaffingRules.MATCH_QUEUE_NOT_IN_SNAPSHOT, 99 as short)
        Match.builder().id(100L).date(MATCH_DATE).build()  | referee(1L)               | String.format(StaffingRules.MATCH_QUEUE_NOT_IN_SNAPSHOT, null)
        match(100L)                                       | referee(77L)              | String.format(StaffingRules.REFEREE_NOT_IN_SNAPSHOT, 77L)
        match(100L)                                       | Referee.builder().build() | String.format(StaffingRules.REFEREE_NOT_IN_SNAPSHOT, null)
    }
}
