package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.GradeConverter;
import com.jamex.refereestaffer.model.dto.GradeDto;
import com.jamex.refereestaffer.model.exception.MatchNotFoundException;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class GradeService {

    private final GradeRepository gradeRepository;
    private final GradeConverter gradeConverter;
    private final MatchRepository matchRepository;

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
}
