package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.VacationConverter;
import com.jamex.refereestaffer.model.dto.VacationDto;
import com.jamex.refereestaffer.model.exception.RefereeNotFoundException;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.VacationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VacationService {

    private final VacationRepository vacationRepository;
    private final RefereeRepository refereeRepository;
    private final VacationConverter vacationConverter;

    public VacationService(VacationRepository vacationRepository, RefereeRepository refereeRepository,
                           VacationConverter vacationConverter) {
        this.vacationRepository = vacationRepository;
        this.refereeRepository = refereeRepository;
        this.vacationConverter = vacationConverter;
    }

    /**
     * Resolves the referee the dto points at (404 when there is no such referee) and persists
     * the vacation. Serves both POST and PUT: the two differ only in whether the dto carries an
     * id, which {@code save} upserts on — the same behavior the controller had inline before the
     * id resolution moved out of {@link VacationConverter}.
     */
    public VacationDto saveVacation(VacationDto vacationDto) {
        var referee = refereeRepository.findById(vacationDto.refereeId())
                .orElseThrow(() -> new RefereeNotFoundException(vacationDto.refereeId()));
        var vacation = vacationRepository.save(vacationConverter.convertFromDto(vacationDto, referee));
        return vacationConverter.convertFromEntity(vacation);
    }
}
