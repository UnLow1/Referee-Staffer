package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.VacationDto;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Vacation;
import org.springframework.stereotype.Component;

/**
 * Pure mapping between {@link Vacation} and {@link VacationDto} — no repository access.
 * Resolving {@code refereeId} to a {@link Referee} is the service layer's job
 * ({@link com.jamex.refereestaffer.service.VacationService}), so the dto → entity direction
 * takes the already-resolved referee and only the entity → dto half is implemented.
 */
@Component
public class VacationConverter implements EntityToDtoConverter<Vacation, VacationDto> {

    @Override
    public VacationDto convertFromEntity(Vacation entity) {
        return new VacationDto(
                entity.getId(),
                entity.getReferee().getId(),
                entity.getStartDate(),
                entity.getEndDate());
    }

    public Vacation convertFromDto(VacationDto dto, Referee referee) {
        return new Vacation(dto.id(), referee, dto.startDate(), dto.endDate());
    }
}
