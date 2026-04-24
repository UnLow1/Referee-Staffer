package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.RefereeDto;
import com.jamex.refereestaffer.model.entity.Referee;
import org.springframework.stereotype.Component;

@Component
public class RefereeConverter implements BaseConverter<Referee, RefereeDto> {

    @Override
    public RefereeDto convertFromEntity(Referee entity) {
        return new RefereeDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getExperience());
    }

    @Override
    public Referee convertFromDto(RefereeDto dto) {
        return new Referee(
                dto.getId(),
                dto.getFirstName(),
                dto.getLastName(),
                dto.getEmail(),
                null,
                dto.getExperience(),
                null,
                null,
                null,
                false);
    }
}
