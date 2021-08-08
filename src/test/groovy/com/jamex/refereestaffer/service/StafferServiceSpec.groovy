package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.converter.MatchConverter
import com.jamex.refereestaffer.repository.ConfigurationRepository
import spock.lang.Specification
import spock.lang.Subject

class StafferServiceSpec extends Specification {

    @Subject
    StafferService stafferService

    ConfigurationRepository configurationRepository = Mock()
    MatchConverter matchConverter = Mock()
    MatchService matchService = Mock()
    RefereeService refereeService = Mock()

    def setup() {
        stafferService = new StafferService(configurationRepository, matchConverter, matchService, refereeService)
    }
}
