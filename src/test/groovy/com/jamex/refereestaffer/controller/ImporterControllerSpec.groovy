package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.exception.DownloadFileException
import com.jamex.refereestaffer.model.exception.ImportException
import com.jamex.refereestaffer.model.request.ImportResponse
import com.jamex.refereestaffer.service.DownloadService
import com.jamex.refereestaffer.service.ImporterService
import groovy.json.JsonSlurper
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Execution
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart

// Features must run on one thread: the @SpringBean mocks live in the shared Spring
// context, so concurrent features would attach/stub the same mock instances at once.
@Execution(ExecutionMode.SAME_THREAD)
@WebMvcTest(ImporterController)
class ImporterControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    ImporterService importerService = Mock()

    @SpringBean
    DownloadService downloadService = Mock()

    def "should import data from multipart file and return counters"() {
        given:
        def file = new MockMultipartFile("file", "import.csv", "text/csv", "csv data".bytes)

        when:
        def response = mockMvc.perform(multipart("/api/importer")
                .file(file)
                .param("numberOfQueuesToImport", "20"))
                .andReturn().response

        then:
        1 * importerService.importData({ it.originalFilename == "import.csv" }, 20 as Short) >>
                new ImportResponse(160, 15, 120, 16)
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.matches == 160
        json.referees == 15
        json.grades == 120
        json.teams == 16
    }

    def "should respond 400 with problem detail when the CSV cannot be imported"() {
        given:
        def file = new MockMultipartFile("file", "broken.csv", "text/csv", "not a csv".bytes)

        when:
        def response = mockMvc.perform(multipart("/api/importer")
                .file(file)
                .param("numberOfQueuesToImport", "20"))
                .andReturn().response

        then:
        1 * importerService.importData(_, _) >> { throw new ImportException("broken.csv") }
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == String.format(ImportException.ERROR_MESSAGE, "broken.csv")
    }

    def "should download example file as csv"() {
        when:
        def response = mockMvc.perform(get("/api/importer/example")).andReturn().response

        then:
        1 * downloadService.downloadFile(ImporterController.EXAMPLE_FILENAME) >>
                new ByteArrayResource("home;away;score".bytes)
        response.status == 200
        response.contentType.startsWith("text/csv")
        response.contentAsString == "home;away;score"
    }

    def "should respond 500 with problem detail when the example file cannot be read"() {
        when:
        def response = mockMvc.perform(get("/api/importer/example")).andReturn().response

        then:
        1 * downloadService.downloadFile(_) >> { throw new DownloadFileException("example import.csv") }
        response.status == 500
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == String.format(DownloadFileException.ERROR_MESSAGE, "example import.csv")
    }
}
