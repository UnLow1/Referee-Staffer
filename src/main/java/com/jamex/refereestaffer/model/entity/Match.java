package com.jamex.refereestaffer.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
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
    @Setter
    private Referee referee;

    @OneToOne(mappedBy = "match", targetEntity = Grade.class)
    private Grade grade;

    private Short homeScore;

    private Short awayScore;

    @Transient
    @Setter
    private double hardnessLvl;

    public Match(short queue, Team home, Team away, LocalDateTime date, Referee referee, Short homeScore, Short awayScore) {
        this.queue = queue;
        this.home = home;
        this.away = away;
        this.date = date;
        this.referee = referee;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
}
