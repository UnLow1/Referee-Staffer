package com.jamex.refereestaffer.service

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
}
