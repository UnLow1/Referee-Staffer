package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.GradeConverter;
import com.jamex.refereestaffer.model.dto.GradeDto;
import com.jamex.refereestaffer.model.exception.GradeNotFoundException;
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

    @GetMapping("/{id}")
    public GradeDto getGrade(@PathVariable Long id) {
        log.info("Getting grade with id " + id);
        var grade = gradeRepository.findById(id).orElseThrow(() -> new GradeNotFoundException(id));
        return gradeConverter.convertFromEntity(grade);
    }

    @PostMapping("/{matchId}")
    public void createGrade(@RequestBody GradeDto gradeDto, @PathVariable Long matchId) {
        log.info("Adding new grade for match with id " + matchId);
        gradeService.addGrade(gradeDto, matchId);
    }

    @PutMapping
    public void updateGrade(@RequestBody GradeDto gradeDto) {
        log.info("Updating grade with id " + gradeDto.getId());
        gradeService.updateGrade(gradeDto);
    }

    @PostMapping("/byIds")
    public Collection<GradeDto> getGradesByIds(@RequestBody IDRequest request) {
        log.info("Getting grades with ids: " + request.getIds());
        var grades = gradeRepository.findAllById(request.getIds());
        return gradeConverter.convertFromEntities(grades);
    }

    @DeleteMapping
    public void deleteAll() {
        log.info("Deleting all grades");
        gradeRepository.deleteAll();
    }

    @DeleteMapping("{id}")
    public void deleteGrade(@PathVariable Long id) {
        log.info("Deleting grade with id = " + id);
        gradeRepository.deleteById(id);
    }
}
