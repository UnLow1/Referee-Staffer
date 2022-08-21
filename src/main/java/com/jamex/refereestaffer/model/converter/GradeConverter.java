package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.GradeDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.exception.GradeNotFoundException;
import com.jamex.refereestaffer.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class GradeConverter implements BaseConverter<Grade, GradeDto> {

    private final GradeRepository gradeRepository;

    @Override
    public GradeDto convertFromEntity(Grade entity) {
        return GradeDto.builder()
                .id(entity.getId())
                .value(entity.getValue())
                .build();
    }

    @Override
    public Grade convertFromDto(GradeDto dto) {
        Match match = null;
        // TODO remove if?
        if (dto.getId() != null) {
            match = gradeRepository.findById(dto.getId())
                    .map(Grade::getMatch)
                    .orElseThrow(() -> new GradeNotFoundException(dto.getId()));
        }
        return Grade.builder()
                .id(dto.getId())
                .value(dto.getValue())
                .match(match)
                .build();
    }
}
