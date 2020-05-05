package com.jamex.refereestaffer.model.dto;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
@Builder
public class MatchDto {

    @NotNull
    private final Short queue;

    @NotNull
    private final Long homeTeamId;

    @NotNull
    private final Long awayTeamId;

    private final Long refereeId;

    private final Short homeScore;

    private final Short awayScore;

    private final Long gradeId;
}
