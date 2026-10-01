package com.jamex.refereestaffer.service

import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification
import spock.lang.Subject

/**
 * Covers the short codes the importer stores (RS-78). Kept apart from ImporterServiceSpec,
 * which asserts interaction counts for the whole CSV pipeline — these features only care
 * about what lands in `short_code`.
 */
class ImporterServiceTeamShortCodeSpec extends Specification {

    private static final String HEADER = "queue;home;away;date;referee;homeScore;awayScore;grade"

    @Subject
    ImporterService importerService

    TeamRepository teamRepository = Mock()
    RefereeRepository refereeRepository = Mock()
    MatchRepository matchRepository = Mock()
    GradeRepository gradeRepository = Mock()

    def setup() {
        importerService = new ImporterService(teamRepository, refereeRepository, matchRepository, gradeRepository)
    }

    private List<Team> importAndCaptureTeams(String... rows) {
        List<Team> saved = null
        def csv = ([HEADER] + (rows as List)).join("\n")
        def file = new MockMultipartFile("file", "teams.csv", "text/csv", csv.getBytes("UTF-8"))

        teamRepository.saveAll(_) >> { args -> saved = args[0] as List<Team>; saved }
        teamRepository.findByName(_) >> Optional.of(new Team())
        refereeRepository.findByFirstNameAndLastName(_, _) >> Optional.of(new Referee())
        teamRepository.findAll() >> []
        refereeRepository.findAll() >> []
        matchRepository.findAll() >> []
        gradeRepository.findAll() >> []

        importerService.importData(file, 30 as short)
        return saved
    }

    def "should store a generated short code for every imported team"() {
        when:
        def teams = importAndCaptureTeams("1;Legia;Wisla;01.01.2025 12:00;John Smith;1;0;8.3")

        then: "the column is populated, not left to the name-derived fallback"
        teams*.name == ["Legia", "Wisla"]
        teams*.shortCodeOverride == ["LEG", "WIS"]
    }

    def "should give colliding team names distinct short codes"() {
        when: "Lech and Lechia share a three-letter prefix"
        def teams = importAndCaptureTeams(
                "1;Lech;Lechia;01.01.2025 12:00;John Smith;1;0;8.3")

        then: "the second one slides off the shared prefix instead of duplicating it"
        teams*.name == ["Lech", "Lechia"]
        teams*.shortCodeOverride == ["LEC", "LEH"]
        and: "which is the whole point - the rendered codes differ too"
        teams*.shortCode.toSet().size() == 2
    }

    def "should assign codes in CSV order so the same file always imports identically"() {
        when: "the colliding pair arrives in the opposite order"
        def teams = importAndCaptureTeams(
                "1;Lechia;Lech;01.01.2025 12:00;John Smith;1;0;8.3")

        then: "first appearance wins the plain prefix - home columns before away columns"
        teams*.name == ["Lechia", "Lech"]
        and: "so the codes swap owners compared with the opposite ordering"
        teams*.shortCodeOverride == ["LEC", "LEH"]
    }

    def "should deduplicate team names appearing in several rows"() {
        when:
        def teams = importAndCaptureTeams(
                "1;Legia;Wisla;01.01.2025 12:00;John Smith;1;0;8.3",
                "2;Wisla;Legia;08.01.2025 12:00;John Smith;0;2;8.1")

        then: "each team is created once, so no code is wasted on a repeat"
        teams*.name == ["Legia", "Wisla"]
        teams*.shortCodeOverride == ["LEG", "WIS"]
    }

    def "should leave the override empty when a team name has no alphanumeric character"() {
        when:
        def teams = importAndCaptureTeams("1;???;Wisla;01.01.2025 12:00;John Smith;1;0;8.3")

        then: "no override is invented - the entity keeps its own fallback behaviour"
        teams*.name == ["???", "Wisla"]
        teams[0].shortCodeOverride == null
        teams[0].shortCode == ""
        teams[1].shortCodeOverride == "WIS"
    }

    def "should generate distinct codes across the whole real import file"() {
        given:
        def file = new File("data/import data file.csv")
        def multipartFile = new MockMultipartFile("file", new FileInputStream(file))
        List<Team> saved = null

        teamRepository.saveAll(_) >> { args -> saved = args[0] as List<Team>; saved }
        teamRepository.findByName(_) >> Optional.of(new Team())
        refereeRepository.findByFirstNameAndLastName(_, _) >> Optional.of(new Referee())
        teamRepository.findAll() >> []
        refereeRepository.findAll() >> []
        matchRepository.findAll() >> []
        gradeRepository.findAll() >> []

        when:
        importerService.importData(multipartFile, 30 as short)

        then: "every team gets a code and no two teams share one"
        saved.size() > 1
        saved.every { it.shortCodeOverride != null }
        saved*.shortCodeOverride.toSet().size() == saved.size()
        and: "codes fit the short_code column"
        saved.every { it.shortCodeOverride.length() <= 8 }
    }
}
