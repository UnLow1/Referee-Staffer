package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
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

    static final String HEADER = "queue;home;away;date;referee;homeScore;awayScore;grade"

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
        // 10 of the 240 rows end with an empty grade cell, which the parser reads as "no grade"
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

    def "should import split grade from CSV"() {
        given:
        MultipartFile multipartFile = csv("1;Team1;Team2;01.01.2025 12:00;John Smith;1;0;7.9/8.3")

        when:
        importerService.importData(multipartFile, 30 as short)

        then:
        1 * teamRepository.saveAll(_)
        1 * refereeRepository.saveAll(_)
        2 * teamRepository.findByName(_) >> Optional.of(new Team())
        1 * refereeRepository.findByFirstNameAndLastName("John", "Smith") >> Optional.of(new Referee())
        1 * matchRepository.save(_)
        1 * gradeRepository.save({ Grade grade ->
            grade.value == 7.9d && grade.secondValue == 8.3d && grade.effectiveValue == (7.9d + 8.3d) / 2
        })
        2 * matchRepository.findAll() >> []
        1 * refereeRepository.findAll() >> []
        2 * gradeRepository.findAll() >> []
        1 * teamRepository.findAll() >> []
    }

    def "should create every team and referee exactly once regardless of how often rows repeat them"() {
        given:
        MultipartFile multipartFile = csv(
                "1;Team1;Team2;01.01.2025 12:00;John Smith;1;0;8.3",
                "2;Team2;Team1;08.01.2025 12:00;John Smith;2;2;8.1",
                "3;Team3;Team1;15.01.2025 12:00;Ann Brown;0;1;8.0")

        when:
        importerService.importData(multipartFile, 30 as short)

        then:
        // LinkedHashSet keeps the order the file introduces the teams in
        1 * teamRepository.saveAll({ List<Team> teams ->
            teams*.name == ["Team1", "Team2", "Team3"]
        })
        1 * refereeRepository.saveAll({ List<Referee> referees ->
            referees.collect { "$it.firstName $it.lastName" } == ["John Smith", "Ann Brown"]
        })
        6 * teamRepository.findByName(_) >> Optional.of(new Team())
        3 * refereeRepository.findByFirstNameAndLastName(_, _) >> Optional.of(new Referee())
        3 * matchRepository.save(_)
        3 * gradeRepository.save(_)
        2 * matchRepository.findAll() >> []
        1 * refereeRepository.findAll() >> []
        2 * gradeRepository.findAll() >> []
        1 * teamRepository.findAll() >> []
    }

    def "should import a fixture with no referee, result or grade as a bare match"() {
        given:
        MultipartFile multipartFile = csv("5;Team1;Team2;01.03.2025 12:00;;;;")

        when:
        importerService.importData(multipartFile, 30 as short)

        then:
        1 * teamRepository.saveAll(_)
        1 * refereeRepository.saveAll({ List<Referee> referees -> referees.isEmpty() })
        2 * teamRepository.findByName(_) >> Optional.of(new Team())
        0 * refereeRepository.findByFirstNameAndLastName(_, _)
        1 * matchRepository.save({ Match match ->
            match.queue == 5 as short && match.referee == null &&
                    match.homeScore == null && match.awayScore == null
        })
        0 * gradeRepository.save(_)
        2 * matchRepository.findAll() >> []
        1 * refereeRepository.findAll() >> []
        2 * gradeRepository.findAll() >> []
        1 * teamRepository.findAll() >> []
    }

    def "should import queues above the requested count as fixtures without referee, result or grade"() {
        given:
        MultipartFile multipartFile = csv(
                "1;Team1;Team2;01.01.2025 12:00;John Smith;1;0;8.3",
                "2;Team2;Team1;08.01.2025 12:00;Ann Brown;2;2;8.1")

        when:
        importerService.importData(multipartFile, 1 as short)

        then:
        1 * teamRepository.saveAll(_)
        // both referees are still created — the file names them, the second one just has no match yet
        1 * refereeRepository.saveAll({ List<Referee> referees -> referees.size() == 2 })
        4 * teamRepository.findByName(_) >> Optional.of(new Team())
        1 * refereeRepository.findByFirstNameAndLastName("John", "Smith") >> Optional.of(new Referee())
        0 * refereeRepository.findByFirstNameAndLastName("Ann", "Brown")
        1 * matchRepository.save({ Match match -> match.queue == 1 as short && match.referee != null })
        1 * matchRepository.save({ Match match ->
            match.queue == 2 as short && match.referee == null &&
                    match.homeScore == null && match.awayScore == null
        })
        1 * gradeRepository.save(_)
        2 * matchRepository.findAll() >> []
        1 * refereeRepository.findAll() >> []
        2 * gradeRepository.findAll() >> []
        1 * teamRepository.findAll() >> []
    }

    def "should not touch any repository when the CSV is malformed"() {
        given:
        // The bad row is the last one: the whole file is parsed and validated before the first
        // write, so a problem anywhere in it leaves the database untouched.
        MultipartFile multipartFile = csv(
                "1;Team1;Team2;01.01.2025 12:00;John Smith;1;0;8.3",
                badRow)

        when:
        importerService.importData(multipartFile, 30 as short)

        then:
        def e = thrown(ImportException)
        e.message == String.format(ImportException.ROW_ERROR_MESSAGE, "import.csv", 3L, detail)
        0 * teamRepository._
        0 * refereeRepository._
        0 * matchRepository._
        0 * gradeRepository._

        where:
        badRow                                                      || detail
        "2;Team1;Team2;NOTADATE;John Smith;1;0;8.5"                 || 'date must match dd.MM.yyyy HH:mm but was "NOTADATE"'
        "2;Team1;Team2"                                             || "expected at least 4 columns (queue, home team, away team, date) but got 3"
        "2;Team1;Team2;08.01.2025 12:00;John Smith;1;0;7.9/8.3/8.5" || 'grade must be "8.3" or "7.9/8.3" but was "7.9/8.3/8.5"'
        "2;Team1;Team2;08.01.2025 12:00;John Smith;1;0;8.3/"        || 'grade must be "8.3" or "7.9/8.3" but was "8.3/"'
        "2;Team1;Team2;08.01.2025 12:00;John Smith;1;0;/8.3"        || 'grade must be "8.3" or "7.9/8.3" but was "/8.3"'
        "2;Team1;Team2;08.01.2025 12:00;John Smith;1;;8.3"          || "both team scores must be given or both left empty"
    }

    def "should throw TeamNotFoundException when CSV references a team not in repository"() {
        given:
        MultipartFile multipartFile = csv("1;UnknownTeam;Team2;01.01.2025 12:00;John Smith;1;0;8.5")

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
        MultipartFile multipartFile = csv("1;Team1;Team2;01.01.2025 12:00;Unknown Person;1;0;8.5")

        when:
        importerService.importData(multipartFile, 30 as short)

        then:
        1 * teamRepository.saveAll(_)
        1 * refereeRepository.saveAll(_)
        2 * teamRepository.findByName(_) >> Optional.of(new Team())
        1 * refereeRepository.findByFirstNameAndLastName("Unknown", "Person") >> Optional.empty()
        thrown(RefereeNotFoundException)
    }

    private static MultipartFile csv(String... rows) {
        new MockMultipartFile("file", "import.csv", "text/csv",
                ([HEADER] + rows.toList()).join("\n").getBytes("UTF-8"))
    }
}
