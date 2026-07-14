package com.jamex.refereestaffer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import java.util.List;
import java.util.Map;

@Entity
public class Referee {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column
    private String email;

    @OneToMany(mappedBy = "referee", targetEntity = Match.class)
    private List<Match> matches;

    @Column
    private int experience;

    @Transient
    private Double averageGrade;

    @Transient
    private Short numberOfMatchesInRound;

    @Transient
    private Map<Team, Short> teamsRefereed;

    @Transient
    private boolean busy;

    /**
     * Highest queue this referee has been assigned to. Set by
     * {@link com.jamex.refereestaffer.service.RefereeService#calculateStats}; null when
     * the referee has never been assigned. Surfaced by RefereeDto for the redesigned
     * Referee list and Profile screens.
     */
    @Transient
    private Short lastQueue;

    /**
     * Computed referee strength: P = α·avg(grade) + β·experience. Set by
     * {@link com.jamex.refereestaffer.service.RefereeService#enrichWithStats}; null
     * before the enrichment pass runs.
     */
    @Transient
    private Double potential;

    /** Number of past matches officiated where the home team won. Fairness signal. */
    @Transient
    private Short homeWins;

    /** Number of past matches officiated where the away team won. Fairness signal. */
    @Transient
    private Short awayWins;

    public Referee() {
    }

    public Referee(Long id, String firstName, String lastName, String email, List<Match> matches, int experience,
                   Double averageGrade, Short numberOfMatchesInRound, Map<Team, Short> teamsRefereed, boolean busy) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.matches = matches;
        this.experience = experience;
        this.averageGrade = averageGrade;
        this.numberOfMatchesInRound = numberOfMatchesInRound;
        this.teamsRefereed = teamsRefereed;
        this.busy = busy;
    }

    public Referee(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Referee(String firstName, String lastName, String email, int experience) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.experience = experience;
    }

    public Long getId() {
        return id;
    }

    /**
     * "S C" = "Sędzia z Centrali" (central-level referee assigned top-down by PZPN).
     * The imported CSV stores such assignments with firstName="S", lastName="C" as a
     * sentinel meaning "the staffer must not touch this match". Kept as a name-based
     * sentinel for now — modelling it as an explicit flag is a separate ticket.
     */
    public boolean isCentralSentinel() {
        return "S".equals(firstName) && "C".equals(lastName);
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

    public List<Match> getMatches() {
        return matches;
    }

    public int getExperience() {
        return experience;
    }

    public Double getAverageGrade() {
        return averageGrade;
    }

    public void setAverageGrade(Double averageGrade) {
        this.averageGrade = averageGrade;
    }

    public Short getNumberOfMatchesInRound() {
        return numberOfMatchesInRound;
    }

    public void setNumberOfMatchesInRound(Short numberOfMatchesInRound) {
        this.numberOfMatchesInRound = numberOfMatchesInRound;
    }

    public Map<Team, Short> getTeamsRefereed() {
        return teamsRefereed;
    }

    public void setTeamsRefereed(Map<Team, Short> teamsRefereed) {
        this.teamsRefereed = teamsRefereed;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public Short getLastQueue() {
        return lastQueue;
    }

    public void setLastQueue(Short lastQueue) {
        this.lastQueue = lastQueue;
    }

    public Double getPotential() {
        return potential;
    }

    public void setPotential(Double potential) {
        this.potential = potential;
    }

    public Short getHomeWins() {
        return homeWins;
    }

    public void setHomeWins(Short homeWins) {
        this.homeWins = homeWins;
    }

    public Short getAwayWins() {
        return awayWins;
    }

    public void setAwayWins(Short awayWins) {
        this.awayWins = awayWins;
    }

    @Override
    public String toString() {
        return "Referee(firstName=" + firstName + ", lastName=" + lastName
                + ", experience=" + experience + ", averageGrade=" + averageGrade
                + ", numberOfMatchesInRound=" + numberOfMatchesInRound + ")";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private List<Match> matches;
        private int experience;
        private Double averageGrade;
        private Short numberOfMatchesInRound;
        private Map<Team, Short> teamsRefereed;
        private boolean busy;

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

        public Builder matches(List<Match> matches) {
            this.matches = matches;
            return this;
        }

        public Builder experience(int experience) {
            this.experience = experience;
            return this;
        }

        public Builder averageGrade(Double averageGrade) {
            this.averageGrade = averageGrade;
            return this;
        }

        public Builder numberOfMatchesInRound(Short numberOfMatchesInRound) {
            this.numberOfMatchesInRound = numberOfMatchesInRound;
            return this;
        }

        public Builder teamsRefereed(Map<Team, Short> teamsRefereed) {
            this.teamsRefereed = teamsRefereed;
            return this;
        }

        public Builder busy(boolean busy) {
            this.busy = busy;
            return this;
        }

        public Referee build() {
            return new Referee(id, firstName, lastName, email, matches, experience,
                    averageGrade, numberOfMatchesInRound, teamsRefereed, busy);
        }
    }
}
