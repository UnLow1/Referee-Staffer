package com.jamex.refereestaffer.model.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;

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
    @Email
    private final String email;

    @NotNull
    private final Integer experience;
}
