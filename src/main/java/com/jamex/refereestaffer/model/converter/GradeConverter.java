package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.GradeDto;
import com.jamex.refereestaffer.model.entity.Grade;
import org.springframework.stereotype.Component;

@Component
public class GradeConverter implements BaseConverter<Grade, GradeDto> {

    @Override
    public GradeDto convertFromEntity(Grade entity) {
        return GradeDto.builder()
                .id(entity.getId())
                .value(entity.getValue())
                .build();
    }

    @Override
    public Grade convertFromDto(GradeDto dto) {
        return Grade.builder()
                .id(dto.getId())
                .value(dto.getValue())
                .build();
    }
}
