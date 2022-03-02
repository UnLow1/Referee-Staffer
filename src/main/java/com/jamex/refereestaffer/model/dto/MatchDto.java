package com.jamex.refereestaffer.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Builder
public class MatchDto {

    @NotNull
    private final Long id;

    @NotNull
    private final Short queue;

    @NotNull
    private final Long homeTeamId;

    @NotNull
    private final Long awayTeamId;

    private final LocalDateTime date;

    @Setter
    private Long refereeId;

    private final Short homeScore;

    private final Short awayScore;

    private final Long gradeId;
}
