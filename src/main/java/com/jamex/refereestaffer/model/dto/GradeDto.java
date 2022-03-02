package com.jamex.refereestaffer.model.dto;

import javax.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GradeDto {

    @NotNull
    private final Long id;

    @NotNull
    private final Double value;
}
