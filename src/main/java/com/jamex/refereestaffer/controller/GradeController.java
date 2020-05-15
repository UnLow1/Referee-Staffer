package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.GradeConverter;
import com.jamex.refereestaffer.model.dto.GradeDto;
import com.jamex.refereestaffer.model.request.IDRequest;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeRepository gradeRepository;
    private final GradeConverter gradeConverter;
    private final GradeService gradeService;

    @GetMapping
    public Collection<GradeDto> getGrades() {
        log.info("Getting all grades");
        var grades = gradeRepository.findAll();
        return gradeConverter.convertFromEntities(grades);
    }

    @PostMapping("/{matchId}")
    public void addGrade(@RequestBody GradeDto gradeDto, @PathVariable Long matchId) {
        log.info("Adding new grade");
        gradeService.addGrade(gradeDto, matchId);
    }

    @PostMapping("/byIds")
    public Collection<GradeDto> getGradesByIds(@RequestBody IDRequest request) {
        log.info("Getting grades with ids: " + request.getIds());
        var grades = gradeRepository.findAllById(request.getIds());
        return gradeConverter.convertFromEntities(grades);
    }
}
