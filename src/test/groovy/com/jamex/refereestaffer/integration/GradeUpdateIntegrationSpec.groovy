package com.jamex.refereestaffer.integration

import com.jamex.refereestaffer.model.dto.GradeDto
import com.jamex.refereestaffer.model.entity.Grade
import com.jamex.refereestaffer.model.entity.Match
import com.jamex.refereestaffer.model.entity.Referee
import com.jamex.refereestaffer.model.entity.Team
import com.jamex.refereestaffer.repository.GradeRepository
import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.RefereeRepository
import com.jamex.refereestaffer.repository.TeamRepository
import com.jamex.refereestaffer.service.GradeService
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Execution
import spock.lang.Isolated
import spock.lang.Specification

import java.time.LocalDateTime

/**
 * Regression net for the one path RS-108 could break. {@code updateGrade} hands {@code save}
 * a freshly built, detached {@link Grade} carrying the stored id instead of mutating the loaded
 * managed one, so the write goes through {@code em.merge} as a full-object replace — and
 * {@link Grade#getMatch()} is the owning side of the association ({@code Match.grade} is
 * {@code mappedBy = "match"}). The service reads the stored grade for the sole purpose of
 * carrying that match over, because {@link GradeDto} does not expose it; if that ever stops
 * happening, {@code match_id} is silently nulled and no unit spec with a mocked converter can
 * notice. This one drives the real service against H2 and re-reads the row afterwards.
 *
 * {@code @Isolated} + {@code SAME_THREAD} for the same reason as
 * {@link OpenInViewDisabledIntegrationSpec}: the H2 instance is shared with the other
 * integration specs and setup wipes the domain tables.
 */
@Isolated
@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest
class GradeUpdateIntegrationSpec extends Specification {

    @Autowired GradeService gradeService
    @Autowired TeamRepository teamRepository
    @Autowired RefereeRepository refereeRepository
    @Autowired MatchRepository matchRepository
    @Autowired GradeRepository gradeRepository

    Long matchId
    Long gradeId

    def setup() {
        wipeDomainData()

        def home = teamRepository.save(new Team("Team1", "City1"))
        def away = teamRepository.save(new Team("Team2", "City2"))
        def referee = refereeRepository.save(new Referee("John", "Doe", "john@doe.com", 5))
        def match = matchRepository.save(new Match((short) 1, home, away,
                LocalDateTime.of(2026, 3, 1, 12, 0), referee, (short) 2, (short) 1))
        def grade = gradeRepository.save(Grade.builder().value(8.5d).match(match).build())

        matchId = match.id
        gradeId = grade.id
    }

    def "should keep the match of the updated grade"() {
        when: "a dto that carries no match reference updates the stored grade"
        gradeService.updateGrade(GradeDto.builder().id(gradeId).value(9.0d).build())

        then: "the new value is persisted and the match association survives the merge"
        def stored = gradeRepository.findById(gradeId).orElseThrow()
        stored.value == 9.0d
        stored.secondValue == null
        stored.match != null
        stored.match.id == matchId

        and: "the merge updated the existing row instead of inserting a second grade"
        gradeRepository.count() == 1
    }

    def "should turn a plain grade into a split one and keep its match"() {
        when:
        gradeService.updateGrade(GradeDto.builder().id(gradeId).value(7.9d).secondValue(8.3d).build())

        then:
        def stored = gradeRepository.findById(gradeId).orElseThrow()
        stored.value == 7.9d
        stored.secondValue == 8.3d
        // unrounded on purpose — (7.9 + 8.3) / 2 is 8.100000000000001, rounding is presentation
        stored.effectiveValue == (7.9d + 8.3d) / 2
        stored.match.id == matchId
        gradeRepository.count() == 1
    }

    def cleanup() {
        wipeDomainData()
    }

    private void wipeDomainData() {
        // FK order, and deleteAllInBatch rather than deleteAll — see the note in
        // OpenInViewDisabledIntegrationSpec.wipeDomainData.
        gradeRepository.deleteAllInBatch()
        matchRepository.deleteAllInBatch()
        refereeRepository.deleteAllInBatch()
        teamRepository.deleteAllInBatch()
    }
}
