package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import spock.lang.Specification
import spock.lang.Subject

class ImporterServiceSpec extends Specification {

    @Subject
    ImporterService importerService

    TeamRepository teamRepository = Mock()
    RefereeRepository refereeRepository = Mock()
    MatchRepository matchRepository = Mock()
    GradeRepository gradeRepository = Mock()

    def setup() {
        importerService = new ImporterService(teamRepository, refereeRepository, matchRepository, gradeRepository)
    }
}
