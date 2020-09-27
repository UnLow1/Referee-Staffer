package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.service.ImporterService
import spock.lang.Specification
import spock.lang.Subject

class ImporterControllerSpec extends Specification {

    @Subject
    ImporterController importerController

    ImporterService importerService = Mock()

    def setup() {
        importerController = new ImporterController(importerService)
    }
}
