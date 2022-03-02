package com.jamex.refereestaffer.model.dto;

import javax.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamDto {

    @NotNull
    private final Long id;

    @NotNull
    private final String name;

    @NotNull
    private final String city;

    private final Short points;
}
