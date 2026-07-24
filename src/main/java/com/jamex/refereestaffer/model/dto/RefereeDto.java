package com.jamex.refereestaffer.model.dto;

import com.jamex.refereestaffer.model.validation.OnUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RefereeDto {

    @NotNull(groups = OnUpdate.class)
    private final Long id;

    @NotBlank
    private final String firstName;

    @NotBlank
    private final String lastName;

    @NotBlank
    @Email
    private final String email;

    @NotNull
    private final Integer experience;

    /**
     * Average observer grade across the referee's match history. Null until at least one
     * graded match exists — frontend treats null as "no grades yet". Populated by
     * {@link com.jamex.refereestaffer.service.RefereeService#enrichWithStats} for the
     * read-only endpoints; create/update flow leaves it null.
     */
    private final Double averageGrade;

    /** Highest queue this referee has ever been assigned to. Null when never assigned. */
    private final Short lastQueue;

    /** Computed potential: α·avg + β·experience. Null when not enriched. */
    private final Double potential;

    /**
     * Number of past matches officiated by this referee where the home team won.
     * Together with {@link #awayWins} this is the fairness signal rendered as a
     * side-by-side bar on the Profile screen. Null until enrichment runs.
     */
    private final Short homeWins;

    /** Number of past matches officiated where the away team won. See {@link #homeWins}. */
    private final Short awayWins;

    // Deliberately the only constructor — Jackson resolves @RequestBody JSON through it by
    // parameter names. A second constructor makes creator resolution ambiguous and breaks
    // deserialization of POST/PUT bodies.
    public RefereeDto(Long id, String firstName, String lastName, String email, Integer experience,
                      Double averageGrade, Short lastQueue, Double potential,
                      Short homeWins, Short awayWins) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.experience = experience;
        this.averageGrade = averageGrade;
        this.lastQueue = lastQueue;
        this.potential = potential;
        this.homeWins = homeWins;
        this.awayWins = awayWins;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public Integer getExperience() {
        return experience;
    }

    public Double getAverageGrade() {
        return averageGrade;
    }

    public Short getLastQueue() {
        return lastQueue;
    }

    public Double getPotential() {
        return potential;
    }

    public Short getHomeWins() {
        return homeWins;
    }

    public Short getAwayWins() {
        return awayWins;
    }

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
