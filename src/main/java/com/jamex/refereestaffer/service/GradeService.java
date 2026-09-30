package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.GradeConverter;
import com.jamex.refereestaffer.model.dto.GradeDto;
import com.jamex.refereestaffer.model.exception.GradeNotFoundException;
import com.jamex.refereestaffer.model.exception.MatchNotFoundException;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GradeService {

    private final GradeRepository gradeRepository;
    private final GradeConverter gradeConverter;
    private final MatchRepository matchRepository;

    public GradeService(GradeRepository gradeRepository, GradeConverter gradeConverter, MatchRepository matchRepository) {
        this.gradeRepository = gradeRepository;
        this.gradeConverter = gradeConverter;
        this.matchRepository = matchRepository;
    }

    public void addGrade(GradeDto gradeDto, Long matchId) {
        var match = matchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        var grade = gradeConverter.convertFromDto(gradeDto);
        grade.setMatch(match);
        gradeRepository.save(grade);
    }

    public void updateGrade(GradeDto gradeDto) {
        var grade = gradeConverter.convertFromDto(gradeDto);
        gradeRepository.save(grade);
    }

    /**
     * Deletes a grade, clearing the back-reference on its match first.
     *
     * <p>Nothing references {@code grade} by foreign key — {@code grade.match_id} points
     * the other way — so this looks like a plain delete, but it is not. {@code Match.grade}
     * is the inverse side of an eager one-to-one, so loading the grade pulls in its match,
     * which points straight back at the row being removed; Hibernate's flush-time transient
     * reference check then rejects the commit (a 500 for the caller). Nulling the reference
     * writes no column, it only keeps the session consistent with the delete.
     */
    public void deleteGrade(Long id) {
        var grade = gradeRepository.findById(id).orElseThrow(() -> new GradeNotFoundException(id));
        var match = grade.getMatch();
        if (match != null) {
            match.setGrade(null);
        }
        gradeRepository.delete(grade);
    }
}
