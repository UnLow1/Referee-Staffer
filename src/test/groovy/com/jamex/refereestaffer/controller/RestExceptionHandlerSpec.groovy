package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.exception.DownloadFileException
import com.jamex.refereestaffer.model.exception.GradeNotFoundException
import com.jamex.refereestaffer.model.exception.ImportException
import com.jamex.refereestaffer.model.exception.MatchNotFoundException
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException
import com.jamex.refereestaffer.model.exception.StafferException
import com.jamex.refereestaffer.model.exception.TeamNotFoundException
import com.jamex.refereestaffer.model.exception.VacationNotFoundException
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class RestExceptionHandlerSpec extends Specification {

    @Subject
    RestExceptionHandler handler = new RestExceptionHandler()

    @Unroll
    def "should map #exception.class.simpleName to 404 NOT_FOUND"() {
        when:
        def problem = handler.handleNotFound(exception)

        then:
        problem.status == HttpStatus.NOT_FOUND.value()
        problem.detail == exception.message

        where:
        exception << [
                new MatchNotFoundException(1L),
                new RefereeNotFoundException(2L),
                new TeamNotFoundException(3L),
                new GradeNotFoundException(4L),
                new VacationNotFoundException(5L)
        ]
    }

    def "should map StafferException to 409 CONFLICT"() {
        given:
        def exception = new StafferException()

        when:
        def problem = handler.handleStafferConflict(exception)

        then:
        problem.status == HttpStatus.CONFLICT.value()
        problem.detail == exception.message
    }

    def "should map ImportException to 400 BAD_REQUEST"() {
        given:
        def exception = new ImportException("file.csv")

        when:
        def problem = handler.handleImport(exception)

        then:
        problem.status == HttpStatus.BAD_REQUEST.value()
        problem.detail == exception.message
    }

    def "should map DownloadFileException to 500 INTERNAL_SERVER_ERROR"() {
        given:
        def exception = new DownloadFileException("file.csv")

        when:
        def problem = handler.handleDownload(exception)

        then:
        problem.status == HttpStatus.INTERNAL_SERVER_ERROR.value()
        problem.detail == exception.message
    }
}
