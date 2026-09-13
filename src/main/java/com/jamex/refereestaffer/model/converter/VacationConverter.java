package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.VacationDto;
import com.jamex.refereestaffer.model.entity.Vacation;
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException;
import com.jamex.refereestaffer.repository.RefereeRepository;
import org.springframework.stereotype.Component;

@Component
public class VacationConverter implements BaseConverter<Vacation, VacationDto> {

    private final RefereeRepository refereeRepository;

    public VacationConverter(RefereeRepository refereeRepository) {
        this.refereeRepository = refereeRepository;
    }

    @Override
    public VacationDto convertFromEntity(Vacation entity) {
        return new VacationDto(
                entity.getId(),
                entity.getReferee().getId(),
                entity.getStartDate(),
                entity.getEndDate());
    }

    @Override
    public Vacation convertFromDto(VacationDto dto) {
        var referee = refereeRepository.findById(dto.refereeId())
                .orElseThrow(() -> new RefereeNotFoundException(dto.refereeId()));

        return new Vacation(dto.id(), referee, dto.startDate(), dto.endDate());
    }
}
