package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.dto.MatchDto
import com.jamex.refereestaffer.service.StafferService
import spock.lang.Specification
import spock.lang.Subject

class StafferControllerSpec extends Specification {

    @Subject
    StafferController stafferController

    StafferService stafferService = Mock()

    def setup() {
        stafferController = new StafferController(stafferService)
    }

    def "should staff referees to matches in provided queue"() {
        given:
        short queue = 12
        def matchesInQueue = [MatchDto.builder().build(), MatchDto.builder().build()]

        when:
        def result = stafferController.staffReferees(queue)

        then:
        1 * stafferService.staffReferees(queue) >> matchesInQueue
        result == matchesInQueue
    }
}
