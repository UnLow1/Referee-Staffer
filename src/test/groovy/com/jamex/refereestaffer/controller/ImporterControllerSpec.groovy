package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.service.ImporterService
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.Subject

class ImporterControllerSpec extends Specification {

    @Subject
    ImporterController importerController

    ImporterService importerService = Mock()

    def setup() {
        importerController = new ImporterController(importerService)
    }

    def "should import data"() {
        given:
        def multipartFile = [getOriginalFilename: { "filename" } ] as MultipartFile
        short numberOfQueuesToImport = 20

        when:
        importerController.importData(multipartFile, numberOfQueuesToImport)

        then:
        1 * importerService.importData(multipartFile, numberOfQueuesToImport)
    }
}
