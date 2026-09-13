package com.jamex.refereestaffer.model.dto;

import com.jamex.refereestaffer.model.validation.OnUpdate;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record MatchDto(

        @NotNull(groups = OnUpdate.class)
        Long id,

        @NotNull
        Short queue,

        @NotNull
        Long homeTeamId,

        @NotNull
        Long awayTeamId,

        LocalDateTime date,

        Long refereeId,

        Short homeScore,

        Short awayScore,

        Long gradeId,

        /**
         * Computed match difficulty (the entity field is @Transient, recalculated on every
         * staffing run). Exposed so the Staffer screen can sort and visualise; the
         * per-component breakdown lives on GET /api/matches/{id}/difficulty.
         */
        Double hardnessLvl
) {

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
        private Double hardnessLvl;

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

        public Builder hardnessLvl(Double hardnessLvl) {
            this.hardnessLvl = hardnessLvl;
            return this;
        }

        public MatchDto build() {
            return new MatchDto(id, queue, homeTeamId, awayTeamId, date, refereeId, homeScore, awayScore, gradeId, hardnessLvl);
        }
    }
}
