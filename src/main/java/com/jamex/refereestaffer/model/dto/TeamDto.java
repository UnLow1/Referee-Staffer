package com.jamex.refereestaffer.model.dto;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
@Builder
public class TeamDto {

    @NotNull
    private final Long id;

    @NotNull
    private final String name;

    @NotNull
    private final String city;

}
