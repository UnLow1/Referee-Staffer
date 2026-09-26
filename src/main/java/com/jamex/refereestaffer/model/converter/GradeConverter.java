package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.GradeDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import org.springframework.stereotype.Component;

/**
 * Pure mapping between {@link Grade} and {@link GradeDto} — no repository access. The dto
 * carries no match reference, so the {@link Match} a grade belongs to is resolved by
 * {@link com.jamex.refereestaffer.service.GradeService} and handed in, which is why this
 * converter implements only the entity → dto half.
 */
@Component
public class GradeConverter implements EntityToDtoConverter<Grade, GradeDto> {

    @Override
    public GradeDto convertFromEntity(Grade entity) {
        return new GradeDto(entity.getId(), entity.getValue(), entity.getSecondValue());
    }

    public Grade convertFromDto(GradeDto dto, Match match) {
        return new Grade(dto.id(), dto.value(), dto.secondValue(), match);
    }
}
