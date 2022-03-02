package com.jamex.refereestaffer.model.dto;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
