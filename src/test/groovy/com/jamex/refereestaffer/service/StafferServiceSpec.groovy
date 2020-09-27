package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.converter.MatchConverter
import com.jamex.refereestaffer.repository.ConfigurationRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import spock.lang.Specification
import spock.lang.Subject

class StafferServiceSpec extends Specification {

    @Subject
    StafferService stafferService

    RefereeRepository refereeRepository = Mock()
    MatchRepository matchRepository = Mock()
    ConfigurationRepository configurationRepository = Mock()
    MatchConverter matchConverter = Mock()
    MatchService matchService = Mock()
    TeamRepository teamRepository = Mock()

    def setup() {
        stafferService = new StafferService(refereeRepository, matchRepository, configurationRepository,
                matchConverter, matchService, teamRepository)
    }
}
