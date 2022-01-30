package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.exception.DownloadFileException
import spock.lang.Specification
import spock.lang.Subject

class DownloadServiceSpec extends Specification {

    @Subject
    DownloadService downloadService = new DownloadService()

    def "should download file with provided filename"() {
        given:
        def filename = "test file.csv"

        when:
        def result = downloadService.downloadFile(filename)

        then:
        result.exists()
        result.isReadable()
    }

    def "should throw DownloadFileException if file which should be downloaded doesn't exist"() {
        given:
        def filename = "wrong filename.csv"

        when:
        downloadService.downloadFile(filename)

        then:
        def exception = thrown(DownloadFileException)
        exception.message == String.format(DownloadFileException.ERROR_MESSAGE, filename)
    }
}
