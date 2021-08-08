package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.Subject

class ImporterServiceSpec extends Specification {

    @Subject
    ImporterService importerService

    TeamRepository teamRepository = Mock()
    RefereeRepository refereeRepository = Mock()
    MatchRepository matchRepository = Mock()
    GradeRepository gradeRepository = Mock()

    def setup() {
        importerService = new ImporterService(teamRepository, refereeRepository, matchRepository, gradeRepository)
    }

    // TODO create separate file for this test
    // TODO add tests for exceptions (TeamNotFoundException, RefereeNotFoundException)
    def "should import data from file"() {
        given:
        def file = new File("data/import data file.csv")
        MultipartFile multipartFile = new MockMultipartFile("file", new FileInputStream(file))
        short numberOfQueuesToImport = 30

        when:
        importerService.importData(multipartFile, numberOfQueuesToImport)

        then:
        1 * teamRepository.saveAll(_)
        1 * refereeRepository.saveAll(_)
        480 * teamRepository.findByName(_) >> Optional.of(new Team())
        240 * refereeRepository.findByFirstNameAndLastName(_, _) >> Optional.of(new Referee())
        240 * matchRepository.save(_)
        230 * gradeRepository.save(_)
        2 * matchRepository.findAll() >> []
        1 * refereeRepository.findAll() >> []
        2 * gradeRepository.findAll() >> []
        1 * teamRepository.findAll() >> []
    }
}
