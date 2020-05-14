package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.GradeConverter;
import com.jamex.refereestaffer.model.dto.GradeDto;
import com.jamex.refereestaffer.model.request.IDRequest;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/grades")
public class GradeController {

    private final GradeRepository gradeRepository;
    private final GradeConverter gradeConverter;
    private final MatchRepository matchRepository;

    @GetMapping
    public Collection<GradeDto> getGrades() {
        log.info("Getting all grades");
        var grades = gradeRepository.findAll();
        return gradeConverter.convertFromEntities(grades);
    }

    @PostMapping("/{matchId}")
    public void addGrade(@RequestBody GradeDto gradeDto, @PathVariable Long matchId) {
        log.info("Adding new grade");
        var grade = gradeConverter.convertFromDto(gradeDto);
        var match = matchRepository.findById(matchId).orElseThrow(); // TODO add custom exception and move logic to GradeService
        grade.setMatch(match);
        gradeRepository.save(grade);
    }

    @PostMapping("/byIds")
    public Collection<GradeDto> getGradesByIds(@RequestBody IDRequest request) {
        log.info("Getting grades with ids: " + request.getIds());
        var grades = gradeRepository.findAllById(request.getIds());
        return gradeConverter.convertFromEntities(grades);
    }
}
