package com.jamex.refereestaffer.model.dto;

import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

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
