package com.jamex.refereestaffer.model.dto;

import com.jamex.refereestaffer.model.validation.OnUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// A record has exactly one (canonical) constructor, so Jackson resolves @RequestBody JSON
// through it by component names — no risk of ambiguous creator resolution.
public record RefereeDto(

        @NotNull(groups = OnUpdate.class)
        Long id,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        @Email
        String email,

        @NotNull
        Integer experience,

        /**
         * Average observer grade across the referee's match history. Null until at least one
         * graded match exists — frontend treats null as "no grades yet". Populated by
         * {@link com.jamex.refereestaffer.service.RefereeService#enrichWithStats} for the
         * read-only endpoints; create/update flow leaves it null.
         */
        Double averageGrade,

        /** Highest queue this referee has ever been assigned to. Null when never assigned. */
        Short lastQueue,

        /** Computed potential: α·avg + β·experience. Null when not enriched. */
        Double potential,

        /**
         * Number of past matches officiated by this referee where the home team won.
         * Together with {@link #awayWins} this is the fairness signal rendered as a
         * side-by-side bar on the redesigned Profile screen. Null until enrichment runs.
         */
        Short homeWins,

        /** Number of past matches officiated where the away team won. See {@link #homeWins}. */
        Short awayWins
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private Integer experience;
        private Double averageGrade;
        private Short lastQueue;
        private Double potential;
        private Short homeWins;
        private Short awayWins;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder experience(Integer experience) {
            this.experience = experience;
            return this;
        }

        public Builder averageGrade(Double averageGrade) {
            this.averageGrade = averageGrade;
            return this;
        }

        public Builder lastQueue(Short lastQueue) {
            this.lastQueue = lastQueue;
            return this;
        }

        public Builder potential(Double potential) {
            this.potential = potential;
            return this;
        }

        public Builder homeWins(Short homeWins) {
            this.homeWins = homeWins;
            return this;
        }

        public Builder awayWins(Short awayWins) {
            this.awayWins = awayWins;
            return this;
        }

        public RefereeDto build() {
            return new RefereeDto(id, firstName, lastName, email, experience,
                    averageGrade, lastQueue, potential, homeWins, awayWins);
        }
    }
}
