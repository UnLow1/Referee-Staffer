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
                entity.getExperience(),
                entity.getAverageGrade(),
                entity.getLastQueue(),
                entity.getPotential(),
                entity.getHomeWins(),
                entity.getAwayWins());
    }

    @Override
    public Referee convertFromDto(RefereeDto dto) {
        return new Referee(
                dto.id(),
                dto.firstName(),
                dto.lastName(),
                dto.email(),
                null,
                dto.experience(),
                null,
                null,
                null,
                false);
    }
}
