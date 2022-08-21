package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.VacationDto;
import com.jamex.refereestaffer.model.entity.Vacation;
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException;
import com.jamex.refereestaffer.repository.RefereeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class VacationConverter implements BaseConverter<Vacation, VacationDto> {

    private final RefereeRepository refereeRepository;

    @Override
    public VacationDto convertFromEntity(Vacation entity) {
        return VacationDto.builder()
                .id(entity.getId())
                .refereeId(entity.getReferee().getId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .build();
    }

    @Override
    public Vacation convertFromDto(VacationDto dto) {
        var referee = refereeRepository.findById(dto.getRefereeId())
                .orElseThrow(() -> new RefereeNotFoundException(dto.getRefereeId()));

        return Vacation.builder()
                .id(dto.getId())
                .referee(referee)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
    }
}
