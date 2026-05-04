package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.model.exception.ImportException
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException
import com.jamex.refereestaffer.model.exception.TeamNotFoundException
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

    def "should return zeros when CSV is empty"() {
        given:
        MultipartFile multipartFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0])

        when:
        def result = importerService.importData(multipartFile, 30 as short)

        then:
        result.matches == 0
        result.referees == 0
        result.grades == 0
        result.teams == 0
        1 * teamRepository.saveAll(_)
        1 * refereeRepository.saveAll(_)
        2 * matchRepository.findAll() >> []
        1 * refereeRepository.findAll() >> []
        2 * gradeRepository.findAll() >> []
        1 * teamRepository.findAll() >> []
        0 * teamRepository.findByName(_)
        0 * refereeRepository.findByFirstNameAndLastName(_, _)
        0 * matchRepository.save(_)
        0 * gradeRepository.save(_)
    }

    def "should throw ImportException when CSV row has missing columns"() {
        given:
        // Header + row with only 3 columns (queue + two team names) — date column missing.
        // createTeams reads line[1] and line[2] OK, but createMatchesAndGrades blows up on
        // line[3] (date) with ArrayIndexOutOfBoundsException.
        def csv = "queue;home;away;date;referee;homeScore;awayScore;grade\n1;Team1;Team2"
        MultipartFile multipartFile = new MockMultipartFile("file", "broken.csv", "text/csv", csv.getBytes())

        when:
        importerService.importData(multipartFile, 30 as short)

        then:
        1 * teamRepository.saveAll(_)
        1 * refereeRepository.saveAll(_)
        2 * teamRepository.findByName(_) >> Optional.of(new Team())
        thrown(ImportException)
    }

    def "should throw ImportException when CSV row has malformed date"() {
        given:
        def csv = "queue;home;away;date;referee;homeScore;awayScore;grade\n1;Team1;Team2;NOTADATE;John Smith;1;0;8.5"
        MultipartFile multipartFile = new MockMultipartFile("file", "bad-date.csv", "text/csv", csv.getBytes())

        when:
        importerService.importData(multipartFile, 30 as short)

        then:
        1 * teamRepository.saveAll(_)
        1 * refereeRepository.saveAll(_)
        2 * teamRepository.findByName(_) >> Optional.of(new Team())
        thrown(ImportException)
    }

    def "should throw TeamNotFoundException when CSV references a team not in repository"() {
        given:
        def csv = "queue;home;away;date;referee;homeScore;awayScore;grade\n1;UnknownTeam;Team2;01.01.2025 12:00;John Smith;1;0;8.5"
        MultipartFile multipartFile = new MockMultipartFile("file", "missing-team.csv", "text/csv", csv.getBytes())

        when:
        importerService.importData(multipartFile, 30 as short)

        then:
        1 * teamRepository.saveAll(_)
        1 * refereeRepository.saveAll(_)
        1 * teamRepository.findByName("UnknownTeam") >> Optional.empty()
        thrown(TeamNotFoundException)
    }

    def "should throw RefereeNotFoundException when CSV references a referee not in repository"() {
        given:
        def csv = "queue;home;away;date;referee;homeScore;awayScore;grade\n1;Team1;Team2;01.01.2025 12:00;Unknown Person;1;0;8.5"
        MultipartFile multipartFile = new MockMultipartFile("file", "missing-ref.csv", "text/csv", csv.getBytes())

        when:
        importerService.importData(multipartFile, 30 as short)

        then:
        1 * teamRepository.saveAll(_)
        1 * refereeRepository.saveAll(_)
        2 * teamRepository.findByName(_) >> Optional.of(new Team())
        1 * refereeRepository.findByFirstNameAndLastName("Unknown", "Person") >> Optional.empty()
        thrown(RefereeNotFoundException)
    }
}
