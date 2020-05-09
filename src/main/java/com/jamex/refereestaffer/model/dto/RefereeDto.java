package com.jamex.refereestaffer.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Team;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.Map;

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

    @JsonIgnore
    @Setter
    private double averageGrade;

    @JsonIgnore
    @Setter
    private short numberOfMatchesInRound;

    @JsonIgnore
    @Setter
    private Map<Team, Short> teamsRefereed;

    @JsonIgnore
    @Setter
    private boolean busy;
}
