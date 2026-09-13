package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.GradeDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.exception.GradeNotFoundException;
import com.jamex.refereestaffer.repository.GradeRepository;
import org.springframework.stereotype.Component;

@Component
public class GradeConverter implements BaseConverter<Grade, GradeDto> {

    private final GradeRepository gradeRepository;

    public GradeConverter(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

    @Override
    public GradeDto convertFromEntity(Grade entity) {
        return new GradeDto(entity.getId(), entity.getValue());
    }

    @Override
    public Grade convertFromDto(GradeDto dto) {
        Match match = null;
        // TODO remove if?
        if (dto.id() != null) {
            match = gradeRepository.findById(dto.id())
                    .map(Grade::getMatch)
                    .orElseThrow(() -> new GradeNotFoundException(dto.id()));
        }
        return new Grade(dto.id(), dto.value(), match);
    }
}
