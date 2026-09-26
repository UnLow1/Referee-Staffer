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
        gradeRepository.save(gradeConverter.convertFromDto(gradeDto, match));
    }

    /**
     * {@link GradeDto} carries no match reference, so the stored grade is read for the single
     * purpose of keeping the match it belongs to — saving the converted entity with a null
     * match would otherwise detach the grade from its match.
     *
     * <p>Requires a non-null {@code gradeDto.id()}: it is the only thing identifying the grade
     * to update, and {@code findById(null)} fails inside Spring Data rather than reporting a
     * 404. The single caller, {@code PUT /api/grades}, enforces it with
     * {@code @Validated(OnUpdate.class)}, which rejects a missing id with a 400 before this
     * method is reached.
     */
    public void updateGrade(GradeDto gradeDto) {
        var match = gradeRepository.findById(gradeDto.id())
                .orElseThrow(() -> new GradeNotFoundException(gradeDto.id()))
                .getMatch();
        gradeRepository.save(gradeConverter.convertFromDto(gradeDto, match));
    }
}
