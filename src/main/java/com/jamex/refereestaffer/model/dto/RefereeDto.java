package com.jamex.refereestaffer.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class RefereeDto {

    @NonNull
    private final String firstName;

    @NonNull
    private final String lastName;

    @NonNull
    private final String email;

    @NonNull
    private final int experience;
}
