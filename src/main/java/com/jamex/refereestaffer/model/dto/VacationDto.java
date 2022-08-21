package com.jamex.refereestaffer.model.dto;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Builder
public class VacationDto {

    @NotNull
    private final Long id;

    @NotNull
    private final Long refereeId;

    @NotNull
    private final LocalDate startDate;

    @NotNull
    private final LocalDate endDate;
}
