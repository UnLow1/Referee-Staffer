package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.converter.GradeConverter;
import com.jamex.refereestaffer.model.dto.GradeDto;
import com.jamex.refereestaffer.model.request.IDRequest;
import com.jamex.refereestaffer.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class GradeController {

    private final GradeRepository gradeRepository;
    private final GradeConverter gradeConverter;

    @GetMapping("/grades")
    public Collection<GradeDto> getGrades() {
        var grades = gradeRepository.findAll();
        return gradeConverter.convertFromEntities(grades);
    }

    @PostMapping("/grades/byIds")
    public Collection<GradeDto> getGradesByIds(@RequestBody IDRequest request) {
        var grades = gradeRepository.findAllById(request.getIds());
        return gradeConverter.convertFromEntities(grades);
    }
}
