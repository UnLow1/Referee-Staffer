package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import com.jamex.refereestaffer.repository.VacationRepository
import spock.lang.Specification

class DataServiceSpec extends Specification {

    def gradeRepository = Mock(GradeRepository)
    def vacationRepository = Mock(VacationRepository)
    def matchRepository = Mock(MatchRepository)
    def refereeRepository = Mock(RefereeRepository)
    def teamRepository = Mock(TeamRepository)

    def dataService = new DataService(gradeRepository, vacationRepository, matchRepository,
            refereeRepository, teamRepository)

    def "should delete every table in foreign key order"() {
        when:
        dataService.clearAllData()

        then: "children first: grade -> match, vacation -> referee, match -> referee/team"
        1 * gradeRepository.deleteAllInBatch()

        then:
        1 * vacationRepository.deleteAllInBatch()

        then:
        1 * matchRepository.deleteAllInBatch()

        then:
        1 * refereeRepository.deleteAllInBatch()

        then:
        1 * teamRepository.deleteAllInBatch()
    }

    def "should count the rows before deleting them"() {
        when:
        def summary = dataService.clearAllData()

        then: "every count has to happen while the rows are still there, or the summary is all zeros"
        1 * gradeRepository.count() >> 4l
        1 * vacationRepository.count() >> 3l
        1 * matchRepository.count() >> 7l
        1 * refereeRepository.count() >> 5l
        1 * teamRepository.count() >> 18l

        then: "only then the deletes"
        1 * gradeRepository.deleteAllInBatch()
        1 * vacationRepository.deleteAllInBatch()
        1 * matchRepository.deleteAllInBatch()
        1 * refereeRepository.deleteAllInBatch()
        1 * teamRepository.deleteAllInBatch()

        and:
        summary.grades() == 4l
        summary.vacations() == 3l
        summary.matches() == 7l
        summary.referees() == 5l
        summary.teams() == 18l
    }

    def "should stop at the failing table instead of wiping the rest"() {
        given:
        matchRepository.deleteAllInBatch() >> { throw new RuntimeException("boom") }

        when:
        dataService.clearAllData()

        then: "the exception propagates so the @Transactional boundary can roll the wipe back"
        thrown(RuntimeException)

        and: "nothing past the failure point is touched — no half-wiped database"
        0 * refereeRepository.deleteAllInBatch()
        0 * teamRepository.deleteAllInBatch()
    }
}
