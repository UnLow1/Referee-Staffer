package com.jamex.refereestaffer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;

@Entity
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Short queue;

    // TODO check optional=false, or OneToOne?
    @ManyToOne(targetEntity = Team.class)
    private Team home;

    @ManyToOne(targetEntity = Team.class)
    private Team away;

    @Column(nullable = false)
    private LocalDateTime date;

    @ManyToOne(targetEntity = Referee.class)
    private Referee referee;

    @OneToOne(mappedBy = "match", targetEntity = Grade.class)
    private Grade grade;

    private Short homeScore;

    private Short awayScore;

    @Transient
    private double hardnessLvl;

    public Match() {
    }

    public Match(Long id, Short queue, Team home, Team away, LocalDateTime date, Referee referee,
                 Grade grade, Short homeScore, Short awayScore, double hardnessLvl) {
        this.id = id;
        this.queue = queue;
        this.home = home;
        this.away = away;
        this.date = date;
        this.referee = referee;
        this.grade = grade;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.hardnessLvl = hardnessLvl;
    }

    public Match(short queue, Team home, Team away, LocalDateTime date, Referee referee, Short homeScore, Short awayScore) {
        this.queue = queue;
        this.home = home;
        this.away = away;
        this.date = date;
        this.referee = referee;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }

    public Long getId() {
        return id;
    }

    public Short getQueue() {
        return queue;
    }

    public Team getHome() {
        return home;
    }

    public Team getAway() {
        return away;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Referee getReferee() {
        return referee;
    }

    public void setReferee(Referee referee) {
        this.referee = referee;
    }

    public Grade getGrade() {
        return grade;
    }

    public Short getHomeScore() {
        return homeScore;
    }

    public Short getAwayScore() {
        return awayScore;
    }

    public double getHardnessLvl() {
        return hardnessLvl;
    }

    public void setHardnessLvl(double hardnessLvl) {
        this.hardnessLvl = hardnessLvl;
    }

    @Override
    public String toString() {
        return "Match(id=" + id + ", queue=" + queue + ", home=" + home + ", away=" + away
                + ", date=" + date + ", referee=" + referee + ", grade=" + grade
                + ", homeScore=" + homeScore + ", awayScore=" + awayScore
                + ", hardnessLvl=" + hardnessLvl + ")";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Short queue;
        private Team home;
        private Team away;
        private LocalDateTime date;
        private Referee referee;
        private Grade grade;
        private Short homeScore;
        private Short awayScore;
        private double hardnessLvl;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder queue(Short queue) {
            this.queue = queue;
            return this;
        }

        public Builder home(Team home) {
            this.home = home;
            return this;
        }

        public Builder away(Team away) {
            this.away = away;
            return this;
        }

        public Builder date(LocalDateTime date) {
            this.date = date;
            return this;
        }

        public Builder referee(Referee referee) {
            this.referee = referee;
            return this;
        }

        public Builder grade(Grade grade) {
            this.grade = grade;
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

        public Builder hardnessLvl(double hardnessLvl) {
            this.hardnessLvl = hardnessLvl;
            return this;
        }

        public Match build() {
            return new Match(id, queue, home, away, date, referee, grade, homeScore, awayScore, hardnessLvl);
        }
    }
}
