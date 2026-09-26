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
     * id. A PUT whose id is not in the database neither updates that id nor 404s — {@code save}
     * merges a detached instance, and Hibernate treats one whose row is missing as transient, so
     * it inserts a new row under a freshly generated id. That is what the controller did inline
     * before the id resolution moved out of {@link VacationConverter}; turning PUT into a real
     * existence check would be a behavior change and is deliberately not part of RS-108.
     */
    public VacationDto saveVacation(VacationDto vacationDto) {
        var referee = refereeRepository.findById(vacationDto.refereeId())
                .orElseThrow(() -> new RefereeNotFoundException(vacationDto.refereeId()));
        var vacation = vacationRepository.save(vacationConverter.convertFromDto(vacationDto, referee));
        return vacationConverter.convertFromEntity(vacation);
    }
}
