package com.jamex.refereestaffer.model.dto;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
@Builder
public class RefereeDto {

    @NotNull
    private final Long id;

    @NotNull
    private final String firstName;

    @NotNull
    private final String lastName;

    @NotNull
    private final String email;

    @NotNull
    private final Integer experience;
}
