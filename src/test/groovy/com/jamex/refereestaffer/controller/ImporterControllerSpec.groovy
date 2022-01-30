package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.service.DownloadService
import com.jamex.refereestaffer.service.ImporterService
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.Subject

class ImporterControllerSpec extends Specification {

    @Subject
    ImporterController importerController

    ImporterService importerService = Mock()
    DownloadService downloadService = Mock()

    def setup() {
        importerController = new ImporterController(importerService, downloadService)
    }

    def "should import data"() {
        given:
        def multipartFile = [getOriginalFilename: { "filename" }] as MultipartFile
        short numberOfQueuesToImport = 20

        when:
        importerController.importData(multipartFile, numberOfQueuesToImport)

        then:
        1 * importerService.importData(multipartFile, numberOfQueuesToImport)
    }

    def "should download example file"() {
        when:
        importerController.download()

        then:
        1 * downloadService.downloadFile(importerController.EXAMPLE_FILENAME)
    }
}
