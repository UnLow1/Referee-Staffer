package com.jamex.refereestaffer.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

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

    private final Long refereeId;

    private final Short homeScore;

    private final Short awayScore;

    private final Long gradeId;

    public MatchDto(Long id, Short queue, Long homeTeamId, Long awayTeamId, LocalDateTime date,
                    Long refereeId, Short homeScore, Short awayScore, Long gradeId) {
        this.id = id;
        this.queue = queue;
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.date = date;
        this.refereeId = refereeId;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.gradeId = gradeId;
    }

    public Long getId() {
        return id;
    }

    public Short getQueue() {
        return queue;
    }

    public Long getHomeTeamId() {
        return homeTeamId;
    }

    public Long getAwayTeamId() {
        return awayTeamId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Long getRefereeId() {
        return refereeId;
    }

    public Short getHomeScore() {
        return homeScore;
    }

    public Short getAwayScore() {
        return awayScore;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Short queue;
        private Long homeTeamId;
        private Long awayTeamId;
        private LocalDateTime date;
        private Long refereeId;
        private Short homeScore;
        private Short awayScore;
        private Long gradeId;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder queue(Short queue) {
            this.queue = queue;
            return this;
        }

        public Builder homeTeamId(Long homeTeamId) {
            this.homeTeamId = homeTeamId;
            return this;
        }

        public Builder awayTeamId(Long awayTeamId) {
            this.awayTeamId = awayTeamId;
            return this;
        }

        public Builder date(LocalDateTime date) {
            this.date = date;
            return this;
        }

        public Builder refereeId(Long refereeId) {
            this.refereeId = refereeId;
            return this;
        }

        public Builder homeScore(Short homeScore) {
            this.homeScore = homeScore;
            return this;
        }

        public Builder awayScore(Short awayScore) {
            this.awayScore = awayScore;
            return this;
        }

        public Builder gradeId(Long gradeId) {
            this.gradeId = gradeId;
            return this;
        }

        public MatchDto build() {
            return new MatchDto(id, queue, homeTeamId, awayTeamId, date, refereeId, homeScore, awayScore, gradeId);
        }
    }
}
