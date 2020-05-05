package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.RefereeDto;
import com.jamex.refereestaffer.model.entity.Referee;
import org.springframework.stereotype.Component;

@Component
public class RefereeConverter implements BaseConverter<Referee, RefereeDto> {

    @Override
    public RefereeDto convertFromEntity(Referee entity) {
        return RefereeDto.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .experience(entity.getExperience())
                .build();
    }

    @Override
    public Referee convertFromDto(RefereeDto dto) {
        return Referee.builder()
                .id(dto.getId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .experience(dto.getExperience())
                .build();
    }
}
