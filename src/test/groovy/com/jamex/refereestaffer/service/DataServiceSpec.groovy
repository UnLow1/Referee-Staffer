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

    def "should report the row counts taken before the wipe"() {
        given:
        gradeRepository.count() >> 4l
        vacationRepository.count() >> 3l
        matchRepository.count() >> 7l
        refereeRepository.count() >> 5l
        teamRepository.count() >> 18l

        when:
        def summary = dataService.clearAllData()

        then:
        summary.grades() == 4l
        summary.vacations() == 3l
        summary.matches() == 7l
        summary.referees() == 5l
        summary.teams() == 18l
    }
}
